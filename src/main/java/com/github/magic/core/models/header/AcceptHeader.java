package com.github.magic.core.models.header;

import java.util.PriorityQueue;

public class AcceptHeader extends Header{
    public AcceptHeader(String key, String value) {
        super(key, value);
    }

    /**
     * <p>Construct priority queue of the representation with the priority value being its quality</p>
     * <p>Note that the representation with the quality of 0.0 (or .0) will be considered non-existed and therefore discarded</p>
     * <h6>For example, we have the following header</h6>
     *
     * <p>Accept-Encoding: br, gzip;q=0.5, delflate;q=0.7</p>
     *
     * <p>The queue returned will be: </p>
     * <pre><b>Value    | Priority value</b></pre>
     * <pre>br       | 1.0</pre>
     * <pre>gzip     | 0.5</pre>
     * <pre>deflate  | 0.7</pre>
     *
     * @return the sorted map between the representation and its Quality
     */
    public PriorityQueue<PriorityString> getRepresentationAndQuality(){
        /*
        I'm using Float for the quality because according to mozilla, this quality precision is only up to 3 decimal place:

        -- https://developer.mozilla.org/en-US/docs/Glossary/Quality_values#examples
        The importance of a value is marked by the suffix ';q=' immediately followed by a value between 0 and 1 included,
        with up to three decimal digits, the highest value denoting the highest priority.
        When not present, the default value is 1. */

        PriorityQueue<PriorityString> returnQueue = new PriorityQueue<>();

        String[] tokens = value.split(","); //Representations are seperated by comma, while the quality is by the colon

        for (String token : tokens){
           int qualityIdx = token.indexOf("q=");

           if (qualityIdx != -1){
               returnQueue.add(
                       new PriorityString(
                               token.substring(0, qualityIdx),
                               Float.parseFloat(token.substring(qualityIdx + 2)) //+2 Because we skip an extra "q=" string before the quality
                       )
               );
           }else
               returnQueue.add(new PriorityString(token, 1.0F)); //No quality means highest quality (= 1.0)
        }

        return returnQueue;
    }


    public record PriorityString(String value, float priority) implements Comparable<PriorityString> {

        @Override
            public int compareTo(PriorityString other) {
                //Ascending order
                return Float.compare(other.priority, this.priority);
            }
        }
}
