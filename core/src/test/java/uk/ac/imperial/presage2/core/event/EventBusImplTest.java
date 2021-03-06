/**
 * 	Copyright (C) 2011 Sam Macbeth <sm1106 [at] imperial [dot] ac [dot] uk>
 *
 * 	This file is part of Presage2.
 *
 *     Presage2 is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Presage2 is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser Public License
 *     along with Presage2.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.imperial.presage2.core.event;

import static org.junit.Assert.*;

import org.junit.Test;

import org.jmock.Mockery;

import uk.ac.imperial.presage2.core.Time;

public class EventBusImplTest {

	static class MockEvent implements Event {
		public Time getTime() {
			return null;
		}
	}

	private int invocationCount = 0;

	class MockEventListener {

		@EventListener
		public void hearMockEvent(MockEvent e) {
			invocationCount++;
		}

	}

	@Test
	public void testEventBusImpl() {

		Mockery context = new Mockery();
		Event fakeEvent = context.mock(Event.class);
		MockEventListener listener = new MockEventListener();

		EventBus eventBus = new EventBusImpl();

		// assert no invocation before subscription.
		eventBus.publish(new MockEvent());
		assertEquals(0, invocationCount);

		// assert invocation after subscription
		eventBus.subscribe(listener);
		eventBus.publish(new MockEvent());
		assertEquals(1, invocationCount);

		// assert no invocation from different event
		eventBus.publish(fakeEvent);
		assertEquals(1, invocationCount);

		// assert no invocation after unsubscribe
		eventBus.unsubscribe(listener);
		eventBus.publish(new MockEvent());
		assertEquals(1, invocationCount);

		// assert one invocation after double subscribe
		eventBus.subscribe(listener);
		eventBus.subscribe(listener);
		eventBus.publish(new MockEvent());
		assertEquals(2, invocationCount);
	}

}
