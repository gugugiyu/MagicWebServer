package core.models.routing_tries;

import core.path_handler.HandlerWithParam;
import core.path_handler.Handler;
import core.consts.HttpMethod;
import core.utils.RegexMatcher;
import core.utils.Trimmer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Node {
    //Endpoint token for this branch
    private String token;

    //The handler that will be executed on matching this current route
    private Handler nodeHandler;

    //The child
    private ArrayList<Node> children;

    //The type of HTTP method
    private HttpMethod httpMethod;

    public Node(HttpMethod method, String token, Handler nodeHandler) {
        this.httpMethod = method;
        this.token = token;
        this.nodeHandler = nodeHandler;
        children = new ArrayList<>();
    }

    public Handler getNodeHandler() {
        return nodeHandler;
    }

    public void setNodeHandler(Handler nodeHandler) {
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

    public void setToken(String token) {
        this.token = token;
    }

    public ArrayList<Node> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<Node> children) {
        this.children = children;
    }

    public void register(HttpMethod method, String endpoint, Handler nodeHandler) {
        //In case the endpoint contains the current URL token
        //For example, the endpoint: "/dog" will contain the root "/"

        //We'll probably needs to trim off the request header here as well
        endpoint = trimRequestParam(endpoint);

        String[] tokens = endpoint.split("/");

        if (tokens.length == 0){
            //Endpoint is at the root "/", so split won't return anything probably
            tokens = new String[]{"/"};
        }

        TraverseNode tNode = new TraverseNode(method, endpoint, nodeHandler);
        Node node = tNode.traverse(this, endpoint, true);

        if (tNode.depthLayer == tokens.length) {
            //In this case, the traversing node manage to reach the max depth of the tries
            //In other words, it traverses the whole endpoint route sufficiently
            //We'll just have to update the handler and method in this case

            //This case typically occurs when the user re-register the same route but with different handler and method
            node.setNodeHandler(nodeHandler);
            node.setHttpMethod(method);
        } else {
            //We continue with the remaining tokens, adding new node with null handler for each nested token

            for (int i = tNode.depthLayer; i < tokens.length - 1; i++) {
                Node newNode = new Node(method, tokens[i], null);
                if (tokens[i].contains("*")){
                    //We want the wildcard to be evaluated at last after all the routes have been checked
                    node.children.add(newNode);

                    //Usually, we won't have to do this, as the wildcard indicates it will match all the case after it, we'll have no more token to iterate through
                    node = node.children.getLast();
                }else{
                    node.children.addFirst(newNode);

                    //Navigate to next node
                    node = node.children.getFirst();
                }
            }

            //And for the last token, we'll simply create the node with the main handler
            //Same with above
            if (tokens[tokens.length - 1].contains("*")){
                node.children.add(new Node(method, tokens[tokens.length - 1], nodeHandler));
            }else{
                node.children.addFirst(new Node(method, tokens[tokens.length - 1], nodeHandler));
            }
        }

    }

    private String trimRequestParam(String endpoint) {
        endpoint = endpoint.trim();
        endpoint = Trimmer.trimLeft(endpoint, '/');
        endpoint = Trimmer.trimRight(endpoint, '/');

        //This helps with the split as our iterator starts at 1, so we need an extra "" to skip (represents the root node)
        endpoint = "/" + endpoint;

        return endpoint;
    }


    public HandlerWithParam find(HttpMethod method, String endpoint) {
        if (endpoint == null) {
            //In case we can't parse out the endpoint, use default GET behavior
            return null;
        }

        endpoint = trimRequestParam(endpoint);

        TraverseNode tNode = new TraverseNode(method, endpoint, null);
        Node node = tNode.traverse(this, endpoint, false);

        if (!endpoint.equals("/") && node.token.equals("/")){
            //This case is to prevent registered root being called when another endpoint is requested but no handler is found
            node.setNodeHandler(null);
        }

        return new HandlerWithParam(node.nodeHandler, tNode.params);
    }

    private static class TraverseNode extends Node {
        protected int depthLayer;

        //Track the path from the wildcard token to the furthest endpoint
        protected String wildcardPath;

        protected Map<String, String> params;

        //Constructor used for register function, which doesn't care about the route params and wildcard
        public TraverseNode(HttpMethod method, String endpoint, Handler nodeHandler) {
            super(method, endpoint, nodeHandler);
            params = new HashMap<>();
            wildcardPath = "";
        }
        /**
         * Traverses the URL routing trie structure to find the deepest node reachable by tokenizing the endpoint.
         *
         * @param endpoint the URL endpoint to traverse, using "/" as the delimiter to split the path.
         * @param isRegister flag tells if the current traverse method is being used for registering or not. Use to avoid mapping into wildcard case
         * @return the deepest node that can be reached by following the tokens in the endpoint.
         */
        private Node traverse(Node root, String endpoint, boolean isRegister) {
            // Split the endpoint into tokens using "/" as the delimiter
            String[] tokens = endpoint.split("/", 0);

            // Start traversal from the current node (this)
            Node returnNode = root;

            //Offsetting by 1 so we don't have to traverse the root node again
            int iterator = 1;

            // Traverse the trie until the end of tokens or until traversal is no longer possible
            while (iterator < tokens.length) {
                String splitToken = tokens[iterator];

                boolean isMatched = false;

                //Since we'll need to match everything after the wildcard, so if it exists, and we don't have anymore children to traverse through
                //We simply return the remaining path

                // Iterate through the children of the current node
                for (Node child : returnNode.children) {
                    //Check the method
                    if (!child.getHttpMethod().equals(getHttpMethod())){
                        continue;
                    }

                    String group = isChildTokenRegex(splitToken, child.token);

                    if (!isRegister && child.token.contains("*")) {
                        //Some regex will have the asterisk
                        //So when traversing we not only need to check if it matches the wildcard case but the regex case also
                        isMatched = (group != null || handleWildcard(child.token, splitToken));

                        //Start tracking the path from here
                        String[] remainingTokens = Arrays.copyOfRange(tokens, iterator, tokens.length);
                        String   joinedEndpoint  = String.join("/", remainingTokens);

                        wildcardPath = wildcardPath + "/" + joinedEndpoint;
                    } else if (child.token.startsWith(":")) {
                        //Route paramameter handling
                        String normalizedToken = child.token.substring(1); //Remove the ":" from the registered route param
                        params.put(normalizedToken, splitToken);

                        isMatched = true;
                    } else {
                        // Regex matching and raw matching as well. As regex can't detect when raw routing path is given
                        // For example, if you register index.html with a static file handler
                        // Requesting index.css will also trigger the above route as they're matched from the word "index"

                        if (group != null && splitToken.equals(child.token))
                            isMatched = true;
                    }

                    if (isMatched) {
                        returnNode = child;
                        break;
                    }
                }

                // If no matching child is found, return the current node
                if (!isMatched) {
                    break;
                }

                // Move to the next token
                iterator++;
            }

            depthLayer = iterator;

            //Add the wildcard path so far as part of the params
            if (params != null)
                params.put("wildcard", wildcardPath);

            // Return the deepest node reached
            return returnNode;
        }

        private boolean handleWildcard(String wildcardToken, String splitToken) {
            //Wildcard handling
            int wildCardIdx = wildcardToken.indexOf("*");

            //Check if is there any affixes
            String prefix = wildcardToken.substring(0, wildCardIdx);

            //Suffix can be out of bound
            String suffix = "";

            if (wildCardIdx + 1 <= wildcardToken.length())
                suffix = wildcardToken.substring(wildCardIdx + 1);

            return splitToken.startsWith(prefix) && splitToken.endsWith(suffix);
        }

        private String isChildTokenRegex(String splitToken, String childToken ){
            RegexMatcher regexMatcher = new RegexMatcher();

            String retGroup;

            if (childToken.startsWith(":")){
                //Regex can also be used in route parameter, and is encapsulated between parentheses (())
                //If the route was syntactically correct, the closing parenthesis should be at the last index
                String regexPart = childToken.substring(Math.max(childToken.indexOf("(") + 1, 0), childToken.length() - 1);

                retGroup = regexMatcher.check(splitToken, regexPart);
            }else{
                retGroup = regexMatcher.check(splitToken, childToken);
            }

            return retGroup;
        }
    }
}
