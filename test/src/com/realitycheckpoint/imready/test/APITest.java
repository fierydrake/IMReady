package com.realitycheckpoint.imready.test;

import com.realitycheckpoint.imready.CreateMeeting;
import com.realitycheckpoint.imready.client.API;
import com.realitycheckpoint.imready.client.APICallFailedException;

public class APITest extends android.test.ActivityInstrumentationTestCase2<CreateMeeting> {
	private static final String TEST_SERVER_URI = "http://192.168.2.6:54321/";
	public APITest() {
		super("com.realitycheckpoint.imready", CreateMeeting.class);
		API.setServerURI(TEST_SERVER_URI);
	}

	public void setUp() throws Exception {
	}
	
	public void tearDown() throws Exception {
	}
	
//	public void testMeetings() {
//		try {
//			assertNotNull("Listing meetings should not return null", API.meetings()); // TODO improve
//		} catch (APICallFailedException e) {
//			fail("API call failed: " + e);
//		}
//	}

//	public void testMeetingParticipants() {
//		try {
//			int meetingId = API.createMeeting("mjt", "My Test Meeting");
//			assertNotNull("Listing meeting participants should not return null", API.meetingParticipants(meetingId)); // TODO improve
//		} catch (APICallFailedException e) {
//			fail("API call failed: " + e);
//		}
//	}

	public void testCreateUser() {
		try {
			API.createUser("testuser", "Mr Test"); // TODO improve (need to clear db)
		} catch (APICallFailedException e) {
			fail("API call failed: " + e);
		}
	}

	public void testCreateMeeting() {
		try {
			int meetingId = API.createMeeting("mjt", "My Test Meeting");
			assertTrue("Creating a meeting should return a valid meeting id (meetingId=" + meetingId + ")", meetingId > 0);
		} catch (APICallFailedException e) {
			fail("API call failed: " + e);
		}
	}

//	public void testAddMeetingParticipant() {
//		try {
//			int meetingId = API.createMeeting("mjt", "My Test Meeting");
//			API.addMeetingParticipant(meetingId, "mjt");
//			List<Participant> participants = API.meetingParticipants(meetingId);
//			boolean found = false;
//			for (Participant p : participants) {
//				if (p.getUser().equals(me)) {
//					found = true;
//					break;
//				}
//			}
//			assertTrue("After adding participant to meeting, they should appear in the participant list", found);
//		} catch (APICallFailedException e) {
//			fail("API call failed: " + e);
//		}
//	}

//	public void testReady() {
//		try {
//			int meetingId = API.createMeeting("My Test Meeting");
//			api.addMeetingParticipant(meetingId, me.getId());
//			List<Participant> participants = api.meetingParticipants(meetingId);
//			Participant found = null;
//			for (Participant p : participants) {
//				if (p.getUser().equals(me)) {
//					found = p;
//					break;
//				}
//			}
//			assertNotNull("After adding participant to meeting, they should appear in the participant list", found);
//			assertFalse("Before setting ready participant should not be ready", found.isReady());
//			api.ready(meetingId);
//			assertTrue("After setting ready participant should be ready", found.isReady());
//		} catch (APICallFailedException e) {
//			fail("API call failed: " + e);
//		}
//	}

}
