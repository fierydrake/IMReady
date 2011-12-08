package com.realitycheckpoint.imready.test;

import java.util.UUID;

import com.realitycheckpoint.imready.CreateMeeting;
import com.realitycheckpoint.imready.client.API;
import com.realitycheckpoint.imready.client.APICallFailedException;

public class APITest extends android.test.ActivityInstrumentationTestCase2<CreateMeeting> {
	private String primaryUserId;
	private String secondaryUserId;

	public APITest() {
		super("com.realitycheckpoint.imready", CreateMeeting.class);
	}

	public void setUp() throws Exception {
		String suffix = UUID.randomUUID().toString().replaceAll("-", "_");
		primaryUserId = "testuserA_" + suffix;
		secondaryUserId = "testuserB_" + suffix;
	}
	
	public void testUserResource() {
		/* 
		 * Create a new user
		 */
		try {
			API.createUser(primaryUserId, "Mr Test");
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create user: " + e);
		}
		
		/*
		 * Test that the user now exists
		 */
		try {
			API.user(primaryUserId);
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to retrieve user: " + e);
		}
		
		/*
		 * Test that the user we didn't create fails
		 */
		try {
			API.user(secondaryUserId);
			fail("User '" + secondaryUserId + "' should not exist");
		} catch (APICallFailedException e) {
			assertTrue("User should not be found", e.getMessage().contains("not found"));
		}
	}
	
	public void testMeetingResource() {
		try {
			API.createUser(primaryUserId, "Mr Test A");
			API.createUser(secondaryUserId, "Mr Test B");
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create user: " + e);
		}
		
		try {
			int meetingId = API.createMeeting(primaryUserId, "Test Meeting");
			assertTrue("Creating a meeting should return a valid meeting id (meetingId=" + meetingId + ")", meetingId > 0);
			API.meeting(meetingId);
//			API.addMeetingParticipant(meetingId, secondaryUserId);
//			API.meetingParticipants(meetingId);
			/* -- Check the paticipant was added */
//			API.userMeetings(secondaryUserId);
			/* -- Check the user is in the meeting */
		} catch (APICallFailedException e) {
			e.printStackTrace();
			fail("Failed to create meeting: " + e);			
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
