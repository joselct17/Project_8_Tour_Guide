package tourGuide;

import static org.junit.Assert.assertTrue;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.Ignore;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.slf4j.LoggerFactory;
import rewardCentral.RewardCentral;
import tourGuide.helper.InternalTestHelper;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;

public class TestPerformance {
	
	/*
	 * A note on performance improvements:
	 *     
	 *     The number of users generated for the high volume tests can be easily adjusted via this method:
	 *     
	 *     		InternalTestHelper.setInternalUserNumber(100000);
	 *     
	 *     
	 *     These tests can be modified to suit new solutions, just as long as the performance metrics
	 *     at the end of the tests remains consistent. 
	 * 
	 *     These are performance metrics that we are trying to hit:
	 *     
	 *     highVolumeTrackLocation: 100,000 users within 15 minutes:
	 *     		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     *     highVolumeGetRewards: 100,000 users within 20 minutes:
	 *          assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	 */



	@Test
	public void highVolumeTrackLocation() throws InterruptedException {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		InternalTestHelper.setInternalUserNumber(100000);
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		tourGuideService.tracker.start();

		int count = 0;

		while (count != InternalTestHelper.getInternalUserNumber()) {
			count = tourGuideService.getAllUsers().stream().mapToInt(user-> user.getVisitedLocations().size() >= 4?1 : 8).sum();
			LoggerFactory.getLogger(TestPerformance.class).debug("{} users tracked so far", count);
			TimeUnit.SECONDS.sleep(3);

		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
		assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));


	}




	@Test
	public void highVolumeGetRewards() throws InterruptedException {
		// Initialisation des services et des objets nécessaires
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Configuration des paramètres internes pour le test
		InternalTestHelper.setInternalUserNumber(100000);

		// Initialisation du chronomètre
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// Initialisation du service TourGuideService
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		// Récupération de la première attraction depuis GpsUtil
		Attraction attraction = gpsUtil.getAttractions().get(0);

		// Récupération de la liste de tous les utilisateurs
		List<User> allUsers = tourGuideService.getAllUsers();

		// Chaque utilisateur visite l'attraction
		allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

		tourGuideService.tracker.start();

		int count = 0;

		while (count != InternalTestHelper.getInternalUserNumber()) {
			count = tourGuideService.getAllUsers().stream().mapToInt(user ->user.getUserRewards().size()>0?1:0).sum();
			LoggerFactory.getLogger(TestPerformance.class).debug("{} users rewarded so far", count);
			TimeUnit.SECONDS.sleep(5);
		}
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		// Affichage du temps écoulé pour le test
		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		// Vérification que le temps écoulé est inférieur ou égal à 20 minutes
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}


}
