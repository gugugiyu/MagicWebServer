package core.models.routing_tries;

import core.consts.HttpMethod;
import core.middleware.Middleware;
import core.path_handler.Handler;
import core.path_handler.HandlerWithParam;
import core.utils.Formatter;
import core.utils.RegexMatcher;

import java.util.*;
import java.util.regex.PatternSyntaxException;

import static core.utils.Formatter.replaceEmptyWithRoot;

public class Node implements Cloneable{
    //Middleware to be called before the main handler
    private List<Middleware> middlewares;
    //Endpoint token for this branch
    private final String token;
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


    public void register(HttpMethod method, String endpoint, ArrayList<Middleware> middlewares, Handler nodeHandler) {
        //In case the endpoint contains the current URL token
        //For example, the endpoint: "/dog" will contain the root "/"

        //We'll probably needs to trim off the request header here as well
        endpoint = trimRequestParam(endpoint);

        //Treat "/" as a token as well
        String[] tokens = endpoint.equals("/") ? new String[]{"/"} : replaceEmptyWithRoot(endpoint.split("/", 0));

        TraverseNode tNode = new TraverseNode(method, endpoint, nodeHandler);
        Node node = tNode.registerTraverse(this, tokens);

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

            if (tokens.length < 2 && tokens[i].startsWith("*")) {
                /* Match all case should be evaluated last */
                node.children.addLast(newNode);
                node = node.children.getLast();
            } else {
                node.children.addFirst(newNode);
                node = node.children.getFirst();
            }
        }

        if (middlewares != null)
            node.setMiddleware(middlewares);
    }

    public HandlerWithParam find(HttpMethod method, String endpoint) {
        if (endpoint == null) {
            //In case we can't parse out the endpoint, use default GET behavior
            return null;
        }

        String[] tokens = endpoint.equals("/") ? new String[]{"/"} : replaceEmptyWithRoot(endpoint.split("/", 0));

        endpoint = trimRequestParam(endpoint);

        TraverseNode tNode = new TraverseNode(method, endpoint, null);
        Node node = tNode.findTraverse(this, tokens);

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
        endpoint = Formatter.trimLeft(endpoint, '/');
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
                    isRegexMatched = isChildTokenRegex(compareStr, child.token) != null;

                    if (!child.getHttpMethod().equals(getHttpMethod()))
                        continue;

                    if (child.token.contains("*")) {
                        //Regexes also have asterisk, but they're interpreted differently
                        //So we'll have make sure which one is matching here

                        isMatched = handleWildcard(iterator, tokens, child.token, compareStr);
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

            // Return the deepest node reached
            return isFound ? returnNode : null;
        }

        private boolean handleWildcard(int iterator, String[] tokens, String wildcardToken, String splitToken) {
            //Wildcard handling
            int wildCardIdx = wildcardToken.indexOf("*");

            //Check if is there any affixes
            String prefix = wildcardToken.substring(0, wildCardIdx);

            //Suffix can be out of bound
            String suffix = "";

            if (wildCardIdx + 1 <= wildcardToken.length())
                suffix = wildcardToken.substring(wildCardIdx + 1);

            if (splitToken.startsWith(prefix) && splitToken.endsWith(suffix)){
                String[] remainingTokens = Arrays.copyOfRange(tokens, iterator, tokens.length);
                String joinedEndpoint = String.join("/", remainingTokens);

                wildcardPath = wildcardPath + "/" + joinedEndpoint;

                return true;
            }

            return false;
        }

        private String isChildTokenRegex(String splitToken, String childToken) {
            RegexMatcher regexMatcher = new RegexMatcher();

            String retGroup;

            if (childToken.startsWith(":")) {
                //Regex can also be used in route parameter, and is encapsulated between parentheses (())
                //If the route was syntactically correct, the closing parenthesis should be at the last index
                childToken = childToken.substring(Math.max(childToken.indexOf("(") + 1, 0), childToken.length() - 1);
            }

            try {
                retGroup = regexMatcher.check(splitToken, childToken);
            } catch (PatternSyntaxException e) {
                return null; //Invalid regex, or regex isn't provided
            }

            return retGroup;
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
