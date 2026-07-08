package games.sushigo;

import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class testCardReveals {

    SGGameState state;
    SGParameters params;
    SGForwardModel fm = new SGForwardModel();

    @Before
    public void setup() {
        params = new SGParameters();
        params.setRandomSeed(1234);
        state = new SGGameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void testTwoDumplings() {
        // first we add three dumplings to player 0's hand
        state.getPlayerHands().get(0).draw();
        state.getPlayerHands().get(0).draw();
        state.getPlayerHands().get(0).draw();
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling));
        state.getPlayerHands().get(0).add(new SGCard(SGCard.SGCardType.Dumpling));
        for (int i = 0; i < 8; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction action = (i % 4 != 0) ?
                    actions.stream().map(a -> (ChooseCard) a)
                            .filter(a -> state.getPlayerHands().get(0).get(a.cardIdx).type == SGCard.SGCardType.Dumpling)
                            .findFirst().orElseThrow(() -> new RuntimeException("No dumpling found")) :
                    actions.get(0);
            fm.next(state, action);
        }
        assertEquals(2, state.getPlayedCards().get(0).stream().filter(c -> c.type == SGCard.SGCardType.Dumpling).count());
        assertEquals(3, state.getGameScore(0), 0.001); // 2 dumplings = 3 points
    }

    @Test
    public void testChopsticksAllowSecondPick() {
        // give player 2 a chopsticks card
        state.getPlayerHands().get(2).draw();
        state.getPlayerHands().get(2).add(new SGCard(SGCard.SGCardType.Chopsticks));
        assertSame(SGCard.SGCardType.Chopsticks, state.getPlayerHands().get(2).get(0).type);

        // we can create a Simultaneous Action that chooses the first card for all players...this should include the just added Chopsticks

        SimultaneousAction action = new SimultaneousAction(
                Map.of(
                        0, new ChooseCard(0, 0, false),
                        1, new ChooseCard(1, 0, false),
                        2, new ChooseCard(2, 0, false),
                        3, new ChooseCard(3, 0, false)
                )
        );

        assertTrue(action.getPlayerActions().get(2) instanceof ChooseCard chooseCard && chooseCard.cardIdx == 0);

        fm.next(state, action);
        for (int i = 0; i < 4; i++)
            assertEquals(1, state.getPlayedCards().get(i).getSize());
        assertFalse(state.isActionInProgress());

        assertEquals(List.of(0, 1, 2, 3), state.getCurrentSimultaneousPlayers());

        // Now we can use the Chopsticks
        action = new SimultaneousAction(
                Map.of(
                        0, new ChooseCard(0, 0, false),
                        1, new ChooseCard(1, 0, false),
                        2, new ChooseCard(2, 0, true),
                        3, new ChooseCard(3, 0, false)
                )
        );

        fm.next(state, action);
        for (int i = 0; i < 4; i++)
            assertEquals(1, state.getPlayedCards().get(i).getSize());
        assertTrue(state.isActionInProgress());
        assertEquals(List.of(2), state.getCurrentSimultaneousPlayers());

        List<AbstractAction> availableActions = fm.computeAvailableActions(state);
        assertEquals(6, availableActions.size());
        for (int i = 0; i < 6; i++) {
            assertTrue(availableActions.get(i) instanceof ChooseCard cc && cc.cardIdx == i+1 && cc.playerId == 2);
        }
        fm.next(state, availableActions.get(3)); // pick the 4th card in hand (index 3)
        assertFalse(state.isActionInProgress());

        for (int i = 0; i < 4; i++)
            assertEquals(2, state.getPlayedCards().get(i).getSize());

        assertEquals(List.of(0, 1, 2, 3), state.getCurrentSimultaneousPlayers());
    }
}
