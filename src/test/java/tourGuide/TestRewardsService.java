package tourGuide;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;

public class TestRewardsService {

	@Test
	public void userGetRewards()  {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		InternalTestHelper.setInternalUserNumber(0);

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		Attraction attraction = gpsUtil.getAttractions().get(0);

		user.addToVisitedLocations(new VisitedLocation(user.getUserId(), attraction, new Date()));

		rewardsService.calculateRewards(user);
		rewardsService.waitAllWorkCompleted();

		List<UserReward> userRewards = user.getUserRewards();

		tourGuideService.tracker.stopTracking();

		assertTrue(userRewards.size() == 1);
	}

	
	@Test
	public void isWithinAttractionProximity() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		Attraction attraction = gpsUtil.getAttractions().get(0);
		assertTrue(rewardsService.isWithinAttractionProximity(attraction, attraction));
	}


	@Test
	public void nearAllAttractionsTest() {
		// Crée une instance de GpsUtil et RewardsService avec une proximité maximale
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		rewardsService.setProximityBuffer(Integer.MAX_VALUE);

		// Configure le nombre d'utilisateurs internes à 1
		InternalTestHelper.setInternalUserNumber(1);

		// Crée une instance de TourGuideService
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		// Crée une copie de la liste des utilisateurs
		List<User> users = new ArrayList<>(tourGuideService.getAllUsers());

		// Parcours chaque utilisateur et calcule les récompenses
		for (User user : users) {
			rewardsService.calculateRewards(user);
		}

		// Attend la fin de tous les calculs de récompenses
		rewardsService.waitAllWorkCompleted();

		// Récupère l'utilisateur de test (premier utilisateur)
		User testUser = tourGuideService.getAllUsers().get(0);

		// Récupère les récompenses de l'utilisateur de test
		List<UserReward> userRewards = tourGuideService.getUserRewards(testUser);

		// Arrête le suivi
		tourGuideService.tracker.stopTracking();

		// Vérifie que le nombre de récompenses correspond au nombre d'attractions
		assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
	}
}
