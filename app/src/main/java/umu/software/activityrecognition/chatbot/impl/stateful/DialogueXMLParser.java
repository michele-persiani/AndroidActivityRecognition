package umu.software.activityrecognition.chatbot.impl.stateful;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import umu.software.activityrecognition.chatbot.ChatbotResponse;
import umu.software.activityrecognition.shared.util.xml.XMLParser;


/**
 * XML parser for DialogueFSM
 *
 *
 *
 * <dialogue
 * property1=""
 * property2=""
 * ...
 * >
 *
 *     <states>
 *         <state id=""
 *         destError=""
 *         destReset=""
 *         />
 *     </states>
 *
 *
 *
 *
 *     <transition src="" dest="">                  // src, dest mandatory: source and destination state (int)
 *         <class name="anyMessage|event|jaccard"       // type of transition
 *              event=""                                // event name for event transitions
 *              minSimilarity="" words=""               // min similarity and words for jaccard transitions
 *              />
 *         <response                                // how the transition will build responses, mandatory
 *              reset="true"                        // mandatory. whether the response is reset before being processed
 *              intent=""                           // optional intent classification of the user's prompt
 *              answer=""                           // optional text answer to the user's prompt
 *              action=""                           // optional action to execute
 *              >
 *              <slots>                             // optional
 *                  <slot name="" value=""/>        //name mandatory, value optional. Value can be @prompt to copy the prompt value to the slot
 *              </slots>
 *         </response>
 *     </transition>
 * </dialogue>
 *
 *
 *
 */
public class DialogueXMLParser extends XMLParser<DialogueStateMachine>
{
    public static final String NODE_DIALOGUE    = "dialogue";
    public static final String NODE_TRANSITION  = "transition";
    public static final String NODE_CLASS       = "class";
    public static final String NODE_RESPONSE    = "response";
    public static final String NODE_SLOTS       = "slots";
    public static final String NODE_SLOT        = "slot";
    public static final String NODE_STATES      = "states";
    public static final String NODE_STATE       = "state";

    public static final String ATTR_NAME        = "name";
    public static final String ATTR_VALUE       = "value";

    public static final String ATTR_ID          = "id";
    public static final String ATTR_SRC         = "src";
    public static final String ATTR_DEST        = "dest";
    public static final String ATTR_DEST_ERROR  = "destError";
    public static final String ATTR_DEST_RESET  = "destReset";

    public static final String ATTR_RESET       = "reset";
    public static final String ATTR_ANSWER      = "answer";
    public static final String ATTR_INTENT      = "intent";
    public static final String ATTR_ACTION      = "action";

    public static final String ATTR_MIN_SIM     = "minSimilarity";
    public static final String ATTR_WORDS       = "words";
    public static final String ATTR_WORDS_SEP   = ";";
    public static final String ATTR_EVENT       = "event";

    public static final String CLASS_JACCARD    = "jaccard";
    public static final String CLASS_EVENT      = "event";
    public static final String CLASS_ANY_MSG    = "anyMessage";

    public static final String VALUE_PROMPT     = "@prompt";


    private static class Transition
    {
        Integer src;
        Integer dest;
        IDialogueTransition transition;
    }


    private static class State
    {
        Integer id;
        Integer destError;
        Integer destReset;
    }

    @Override
    protected DialogueStateMachine parseDocument(Document doc)
    {
        Map<String, String> properties = parseProperties(doc);
        List<State> states = parseStates(doc);
        List<Transition> transitions = parseTransitions(doc);


        DialogueStateMachine dialogue = new DialogueStateMachine();

        dialogue.setProperties(properties);

        for (Transition tr : transitions)
            dialogue.setTransition(tr.src, tr.dest, tr.transition);


        for (State s : states)
        {
            if (s.destError != null) dialogue.setErrorState(s.id, s.destError);
            if (s.destReset != null) dialogue.setResetState(s.id, s.destReset);
        }

        return dialogue;
    }

    private Map<String, String> parseProperties(Document doc)
    {
        Map<String, String> attrMap = Maps.newHashMap();
        NamedNodeMap attrNodes = doc.getElementsByTagName(NODE_DIALOGUE).item(0).getAttributes();

        for (int i = 0; i < attrNodes.getLength(); i++)
        {
            Node n = attrNodes.item(i);
            attrMap.put(n.getNodeName(), n.getNodeValue());
        }
        return attrMap;
    }

    private List<State> parseStates(Document doc)
    {
        NodeList ns = doc.getElementsByTagName(NODE_STATES);
        if (ns.getLength() == 0) return Lists.newArrayList();
        Node statesNode = ns.item(0);

        return filterMapChildren(
                statesNode,
                n -> n.getNodeName().equals(NODE_STATE),
                stateNode -> {
                    Integer id        = castAttribute(stateNode, ATTR_ID, true, null, Integer::parseInt);
                    Integer destError = castAttribute(stateNode, ATTR_DEST_ERROR, false, null, Integer::parseInt);
                    Integer destReset = castAttribute(stateNode, ATTR_DEST_RESET, false, null, Integer::parseInt);

                    State stateDef = new State();
                    stateDef.id = id;
                    stateDef.destError = destError;
                    stateDef.destReset = destReset;
                    return stateDef;
                });
    }


    private List<Transition> parseTransitions(Document doc)
    {
        NodeList nodeList = doc.getElementsByTagName(NODE_TRANSITION);
        return filterMap(nodeList, null, node -> {
            int src = castAttribute(node, ATTR_SRC, true, null, Integer::parseInt);
            int dest = castAttribute(node, ATTR_DEST, true, null, Integer::parseInt);

            Transition tr = new Transition();
            tr.src = src;
            tr.dest = dest;
            tr.transition = parseDialogueTransition(node);

            return tr;
        });
    }



    private IDialogueTransition parseDialogueTransition(Node node)
    {
        Node classNode = findChildren(node, NODE_CLASS);
        String cls = getStringAttribute(classNode, ATTR_NAME, true, null);

        Node responseNode = findChildren(node, NODE_RESPONSE);
        assert responseNode != null && classNode != null;

        boolean reset = castAttribute(responseNode, ATTR_RESET, true, true, Boolean::parseBoolean);
        BiConsumer<String, ChatbotResponse> responseConsumer = parseResponse(cls, responseNode);


        switch (cls)
        {
            case CLASS_JACCARD:

                Float minSim = castAttribute(classNode, ATTR_MIN_SIM, true, 0.f, Float::parseFloat);
                String[] words = getStringAttribute(classNode, ATTR_WORDS, true, null).split(ATTR_WORDS_SEP);
                return DialogueTransitionFactory.newJaccardSimilarityMessageTriggered(
                        words,
                        minSim,
                        reset,
                        responseConsumer
                );
            case CLASS_EVENT:
                String eventName = getStringAttribute(classNode, ATTR_EVENT, true, null);
                return DialogueTransitionFactory.newEventTriggered(
                        eventName,
                        reset,
                        responseConsumer
                );
            case CLASS_ANY_MSG:
                return DialogueTransitionFactory.newAnyMessageTriggered(
                        reset,
                        responseConsumer
                );
            default:
                return null;
        }

    }




    private BiConsumer<String, ChatbotResponse> parseResponse(String nodeClass, Node node)
    {
        String answer = getStringAttribute(node, ATTR_ANSWER, false, null);
        String intent = getStringAttribute(node, ATTR_INTENT, false, null);
        String action = getStringAttribute(node, ATTR_ACTION, false, null);

        Map<String, String> slots = Maps.newHashMap();
        Node slotsNode = findChildren(node, NODE_SLOTS);
        if (slotsNode != null)
        {
            filterMapChildren(
                    slotsNode,
                    n -> n.getNodeName().equals(NODE_SLOT),
                    n -> {
                        String name  = getStringAttribute(n, ATTR_NAME, true, null);
                        String value = getStringAttribute(n, ATTR_VALUE, false, null);
                        slots.put(name, value);
                        return n;
                    });
        }

        return (prompt, response) -> {
            if (!nodeClass.equals(CLASS_EVENT))
                response.setPromptText(prompt);
            if (answer != null)
                response.setAnswerText(answer);
            if (action != null)
                response.setAction(action);
            if (intent != null)
                response.setIntent(intent);
            for (String s : slots.keySet())
            {
                String value = slots.get(s);
                if (value != null && value.equals(VALUE_PROMPT))
                    response.setSlot(s, prompt);
                else
                    response.setSlot(s, slots.get(s));
            }
        };
    }


}
