package com.realitycheckpoint.imready.test;

import java.util.List;
import java.util.UUID;

import com.realitycheckpoint.imready.CreateMeeting;
import com.realitycheckpoint.imready.client.API;
import com.realitycheckpoint.imready.client.APICallFailedException;
import com.realitycheckpoint.imready.client.Meeting;
import com.realitycheckpoint.imready.client.Participant;

public class APITest extends android.test.ActivityInstrumentationTestCase2<CreateMeeting> {
	private API api;
	private String primaryUserId;
	private String secondaryUserId;

	public APITest() {
		super("com.realitycheckpoint.imready", CreateMeeting.class);
	}

	public void setUp() throws Exception {
		String suffix = UUID.randomUUID().toString().replaceAll("-", "_");
		primaryUserId = "testuserA_" + suffix;
		secondaryUserId = "testuserB_" + suffix;
		
		api = new API(primaryUserId);
	}
	
	public void testUserResource() {
		/* 
		 * Create a new user
		 */
		try {
			api.createUser(primaryUserId, "Mr Test");
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create user: " + e);
		}
		
		/*
		 * Test that the user now exists
		 */
		try {
			api.user(primaryUserId);
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to retrieve user: " + e);
		}
		
		/*
		 * Test that the user we didn't create fails
		 */
		try {
			api.user(secondaryUserId);
			fail("User '" + secondaryUserId + "' should not exist");
		} catch (APICallFailedException e) {
			assertTrue("User should not be found", e.getMessage().contains("not found"));
		}
	}
	
	public void testMeetingResource() {
		try {
			api.createUser(primaryUserId, "Mr Test A");
			api.createUser(secondaryUserId, "Mr Test B");
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create user: " + e);
		}
		
		int meetingId = 0;
		try {
			meetingId = api.createMeeting(primaryUserId, "Test Meeting");
			assertTrue("Creating a meeting should return a valid meeting id (meetingId=" + meetingId + ")", meetingId > 0);
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create meeting: " + e);
		}
		
		try {
			Meeting meeting = api.meeting(meetingId);
			
			assertEquals("Test Meeting", meeting.getName());
			assertEquals(meetingId, meeting.getId());
			
			// Simple search for now, should implement User.equals() & hashCode()
			Participant found = null;
			List<Participant> participants = meeting.getParticipants();
			for (Participant participant : participants) {
				if (primaryUserId.equals(participant.getUser().getId())) {
					found = participant;
				}
			}
			assertNotNull("Meeting creator should be a participant of meeting", found);
			assertEquals("Mr Test A", found.getUser().getDefaultNickname());
			assertEquals(primaryUserId, found.getUser().getId());
			assertEquals("Meeting creator should be marked as NOTIFIED after meeting creator", true, found.getNotified());
			assertEquals("Meeting creator should be NOT READY after meeting creation", Participant.STATE_NOT_READY, found.getState());
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to read meeting with id '" + meetingId + "': " + e);			
		}
		
		try {
			api.addMeetingParticipant(meetingId, secondaryUserId);
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to add participant to meeting with id '" + meetingId + "': " + e);			
		}

		try {
			Meeting meeting = api.meeting(meetingId);

			// Simple search for now, should implement User.equals() & hashCode()
			Participant found = null;
			List<Participant> participants = meeting.getParticipants();
			for (Participant participant : participants) {
				if (secondaryUserId.equals(participant.getUser().getId())) {
					found = participant;
				}
			}
			assertNotNull("After adding participant they should in the meeting", found);
			assertEquals("Mr Test B", found.getUser().getDefaultNickname());
			assertEquals(secondaryUserId, found.getUser().getId());
			assertEquals("After being added to a meeting, a participant should be marked as NOT NOTIFIED", false, found.getNotified());
			assertEquals("After being added to a meeting, a participant should be NOT READY", Participant.STATE_NOT_READY, found.getState());
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to read meeting with id '" + meetingId + "': " + e);			
		}

		try {
			List<Meeting> meetings = api.userMeetings(secondaryUserId);
			assertEquals("After being added to a meeting, a user should have a meeting in their meeting list", 1, meetings.size());
			
			Meeting found = null;
			for (Meeting meeting : meetings) {
				if (meetingId == meeting.getId()) {
					found = meeting;
				}
			}
			assertNotNull("After being added to a meeting, the meeting should appear in the user's meeting list", found);
			assertEquals(meetingId, found.getId());
			assertEquals("Test Meeting", found.getName());
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to read meeting with id '" + meetingId + "': " + e);			
		}
	}

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
