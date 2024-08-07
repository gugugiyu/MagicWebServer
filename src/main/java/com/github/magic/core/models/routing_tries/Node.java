package com.github.magic.core.models.routing_tries;

import com.github.magic.core.consts.HttpMethod;
import com.github.magic.core.middleware.Middleware;
import com.github.magic.core.path_handler.Handler;
import com.github.magic.core.path_handler.HandlerWithParam;
import com.github.magic.core.utils.Formatter;
import com.github.magic.core.utils.RegexMatcher;

import java.util.*;
import java.util.regex.PatternSyntaxException;

public class Node implements Cloneable{
    //Middleware to be called before the main handler
    private List<Middleware> middlewares;
    //Endpoint token for this branch
    private String token;
    //The handlers that will be executed on matching this current route
    //This is acting as both the handler and the middleware (if passed multiple one, they will be called subsequently)
    private Handler nodeHandler;
    //The child
    private final LinkedList<Node> children;

    //The type of HTTP method
    private HttpMethod httpMethod;

    public Node(HttpMethod method, String token, Handler nodeHandler) {
        this.httpMethod = method;
        this.token = token;
        this.nodeHandler = nodeHandler;
        children = new LinkedList<>();
        middlewares = new ArrayList<>();
    }

    public void setMiddleware(List<Middleware> middlewares){
        this.middlewares = middlewares;
    }

    public Handler getNodeHandlers() {
        return nodeHandler;
    }

    public void setNodeHandlers(Handler nodeHandler) {
        this.nodeHandler = nodeHandler;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getToken() {
        return token;
    }

    public LinkedList<Node> getChildren() {
        return children;
    }

    public Handler getNodeHandler() {
        return nodeHandler;
    }

    public List<Middleware> getMiddlewares() {
        return middlewares;
    }

    public synchronized void register(HttpMethod method, String endpoint, ArrayList<Middleware> middlewares, Handler nodeHandler) {
        endpoint = trimRequestParam(endpoint);

        String[] tokens = endpoint.equals("/") ? new String[]{"/"} : com.github.magic.core.utils.Formatter.replaceEmptyWithRoot(endpoint.split("/", 0));

        TraverseNode tNode = new TraverseNode(method, endpoint, nodeHandler);
        Node node = tNode.registerTraverse(this, tokens);
        Node tempNode = null;

        if (node.getHttpMethod() == method && tNode.depthLayer == tokens.length) {
            if (middlewares != null)
                node.setMiddleware(middlewares);

            node.setNodeHandlers(nodeHandler);
            node.setHttpMethod(method);

            return;
        }

        if (tokens.length == 1 && tokens[0].equals("/") && method != node.getHttpMethod()){
            //Edge case: different method (besides GET) for root path will be treated as the root path's child
            node.children.add(new Node(method, "/", nodeHandler));
            return;
        }

        //We continue with the remaining tokens, adding new node with null handler for each nested token

        for (int i = tNode.depthLayer; i < tokens.length; i++) {
            Node newNode = new Node(
                    method,
                    tokens[i],
                    (i == tokens.length - 1) ? nodeHandler : null //Unless it's the final token, we won't add any handler for the sub-token
            );

            //The route parameter with regex will override the one without

            if (tokens.length < 2 && tokens[i].startsWith("*")) {
                /* Absolute wildcard "*" should be evaluated last */
                node.children.addLast(newNode);
                node = node.children.getLast();
            } else {
                if (tokens[i].startsWith(":")){
                    tempNode = overrideRouteParam(node, tokens[i]);
                    
                    if (tempNode != null){
                        node = tempNode;
                        continue;
                    }
                }

                node.children.addFirst(newNode);
                node = node.children.getFirst();
            }
        }

        if (middlewares != null)
            node.setMiddleware(middlewares);
    }

    /**
     * Override the route parameters at any token level. The {@code afterRoute} will only override (set the token) the current route param in any of these conditions:
     * <ul>
     *     <li>{@code afterRoute} has different name than the current route param</li>
     *     <li>{@code afterRoute} has same name, but different regex</li>
     * </ul>
     *
     * @param node       The current node in which the next token to be inserted
     * @param afterRoute The route to be checked (should be a route parameter, regex is optional)
     * @return null if no route param to be overwritten, or the target node to be overwritten if success
     */
    private Node overrideRouteParam(Node node, String afterRoute) {
        int beforeRegexIdx, afterRegexIdx = afterRoute.indexOf("(");

        String beforeRouteName = "", afterRouteName = afterRoute;
        String afterRouteRegexPart = "";

        for (Node child : node.children) {
            //Ignore routes that aren't route parameters or string literal ":"
            if (!child.token.startsWith(":") || child.token.length() < 2) continue;
            
            beforeRegexIdx = child.token.indexOf("(");

            //Get name for both
            if (beforeRegexIdx != -1){
                beforeRouteName = child.token.substring(0, beforeRegexIdx);
            }else{
                beforeRouteName = child.token;
            }

            if (afterRegexIdx != -1){
                afterRouteName = afterRoute.substring(0, afterRegexIdx);
                afterRouteRegexPart = afterRoute.substring(afterRegexIdx + 1);
            }

            if (beforeRouteName.equals(afterRouteName)){
                if (!afterRouteRegexPart.isEmpty())
                    child.token = afterRoute;

                return child;
            }
        }

        //There's no route param existed yet, just assign it as normal
        return null;
    }

    public HandlerWithParam find(HttpMethod method, String endpoint) {
        if (endpoint == null) {
            //In case we can't parse out the endpoint, use default GET behavior
            return null;
        }

        endpoint = trimRequestParam(endpoint);
        String[] tokens = endpoint.equals("/") ? new String[]{"/"} : com.github.magic.core.utils.Formatter.replaceEmptyWithRoot(endpoint.split("/", 0));


        TraverseNode tNode = new TraverseNode(method, endpoint, null);
        Node node = tNode.findTraverse(this, tokens);

        //Either not found or unable to traverse to the last token
       if (node == null){
           return new HandlerWithParam(
                   null, new HashMap<>(), null
           );
       }

        return new HandlerWithParam(
                 node.nodeHandler,
                tNode.params,
                node.middlewares
        );
    }

    private String trimRequestParam(String endpoint) {
        endpoint = endpoint.trim();
        endpoint = com.github.magic.core.utils.Formatter.trimLeft(endpoint, '/');
        endpoint = Formatter.trimRight(endpoint, '/');

        //This helps with the split as our iterator starts at 1, so we need an extra "" to skip (represents the root node)
        endpoint = "/" + endpoint;

        return endpoint;
    }

    private static class TraverseNode extends Node {
        protected final Map<String, String> params;
        protected int depthLayer;
        //Track the path from the wildcard token to the furthest endpoint
        protected String wildcardPath;

        public TraverseNode(HttpMethod method, String endpoint, Handler nodeHandler) {
            super(method, endpoint, nodeHandler);
            params = new HashMap<>();
            wildcardPath = "";
        }


        private Node registerTraverse(Node root, String[] tokens){
            Node returnNode   = root;
            int  iterator     = 1;

            while (iterator < tokens.length) {
                boolean isMatched = false;
                String compareStr = tokens[iterator];

                for (Node child : returnNode.children) {
                    if (!child.getHttpMethod().equals(getHttpMethod()))
                        continue;

                    if (child.token.startsWith(":") && (":" + compareStr).equals(child.token))
                        isMatched = true;

                    else if (tokens[iterator].equals(child.token))
                        isMatched = true;

                    if (isMatched) {
                        returnNode = child; //Advance to the child
                        break;
                    }
                }

                if (!isMatched) break;

                iterator++;
            }

            depthLayer = iterator;
            return returnNode;
        }

        /**
         * Traverses the URL routing trie structure to find the deepest node reachable by tokenizing the endpoint.
         *
         * @param tokens   the URL endpoint to traverse, using "/" as the delimiter to split the path.
         * @return the deepest node that can be reached by following the tokens in the endpoint.
         */
        private Node findTraverse(Node root, String[] tokens) {
            //Small optimization to skip the loop in case the root path is requested
            if (tokens.length == 1 && getHttpMethod() == root.getHttpMethod())
                return root;

            boolean isFound = false, isMatched, isRegexMatched;
            String compareStr;
            Node returnNode = root;
            int  iterator   = 1;

            //Offsetting by 1, so we don't have to traverse the root node again

            while (iterator < tokens.length) {
                compareStr = tokens[iterator];
                isMatched = false;

                for (Node child : returnNode.children) {
                    isRegexMatched = isTokenRegex(compareStr, child.token) != null;

                    if (!child.getHttpMethod().equals(getHttpMethod()))
                        continue;

                    if (child.token.contains("*")) {
                        //Regexes also have asterisk, but they're interpreted differently
                        //So we'll have make sure which one is matching here

                        isMatched = handleWildcard(iterator, tokens, child.token);
                    } else if (child.token.startsWith(":") && child.token.length() > 1) {
                        //Route parameter without name will be treated as literal string ":"
                        //Skip the colon ":"
                        isMatched = true;

                        if (child.token.contains("("))
                            isMatched = isRegexMatched;

                        if (isMatched)
                            params.put(child.token.substring(1, isRegexMatched ? child.token.indexOf("(") : child.token.length()), compareStr);
                    } else {
                        if (isRegexMatched || compareStr.equals(child.token))
                            isMatched = true;
                    }

                    if (isMatched) {
                        returnNode = child; //Advance to the child
                        isFound = true;
                        break;
                    }
                }

                // If no matching child is found, return the current node
                if (!isMatched) break;

                // Move to the next token
                iterator++;
            }

            depthLayer = iterator;

            //Add the wildcard path so far as part of the params
            if (params != null && !wildcardPath.isEmpty())
                params.put("wildcard", wildcardPath);

            //Only the literal wildcard allows for more sub-routes to be matched with a shorter registered path
            if (wildcardPath.isEmpty() && depthLayer < tokens.length)
                return null;

            // Return the deepest node reached
            return isFound ? returnNode : null;
        }

        /**
         * Handle both the asterisk used within the regexes and the wilcard symbol itself. When the use case of wildcard symbol is detected,
         * the attribute {@code wilcardPath} will be inserted with the value of the remaining path from the matched position.
         * 
         * <br>
         * <br>
         * 
         * <b>Example:</b>
         * Register path: /test/* 
         * <br>
         * Actualy path: "/test/value1/123" 
         * <br>
         * wilcardPath: value1/123 
         * <br>
         * 
         * @see #findTraverse(Node, String[])
         * 
         * @param iterator the index of the current element from {@code tokens} list
         * @param tokens the list of tokens having delimiter of "/"
         * @param wildcardToken the wildcard token regisred in the tree
         * @return true if either regex case or the wildcard symbol matched. Otherwise returns false
         */
        private boolean handleWildcard(int iterator, String[] tokens, String wildcardToken) {
            //Wildcard handling
            int wildCardIdx = wildcardToken.indexOf("*");

            //Check if is there any affixes
            String prefix = wildcardToken.substring(0, wildCardIdx);

            String suffix = "";
            
            //Suffix can be out of bound
            if (wildCardIdx + 1 <= wildcardToken.length())
                suffix = wildcardToken.substring(wildCardIdx + 1);

            if (tokens[iterator].startsWith(prefix) && tokens[iterator].endsWith(suffix)){
                String[] remainingTokens = Arrays.copyOfRange(tokens, iterator, tokens.length);
                String joinedEndpoint = String.join("/", remainingTokens);

                wildcardPath = wildcardPath + "/" + joinedEndpoint;

                return true;
            }

            return false;
        }

        /**
         * <p>Try to get the regex pattern from the {@code regexToken} then compiles it</p>
         * <p>Note that the regex behavior was modified so that it no longer handle direct character matching</p>
         * <b>For example:</b>
         * <p>Regex: test; Data: This is test data</p>
         * <p>Regex compile output: null</p>
         * 
         * @see #findTraverse(Node, String[])
         * 
         * @param compareStr the string to be compiled
         * @param regexToken the string with (potential) regex
         * @return a string matching the regex from {@code compareStr} if found, else return null
         */
        private String isTokenRegex(String compareStr , String regexToken) {
            RegexMatcher regexMatcher = new RegexMatcher();

            String retGroup;

            if (regexToken.startsWith(":")) {
                //Regex can also be used in route parameter, and is encapsulated between parentheses (())
                regexToken = regexToken.substring(
                    Math.max(regexToken.indexOf("(") + 1, 0), 
                    regexToken.length() - 1
                    );
            }

            try {
                retGroup = regexMatcher.check(compareStr, regexToken);
            } catch (PatternSyntaxException e) {
                return null; //Invalid regex, or regex isn't provided
            }

            return compareStr.contains(regexToken) ? null : retGroup;
        }
    }

    @Override
    protected Node clone() {
        try {
            return (Node) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
