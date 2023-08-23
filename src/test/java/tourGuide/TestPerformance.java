package tourGuide;

import static org.junit.Assert.assertTrue;

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
	public void highVolumeTrackLocation() {
		// Initialisation des services et des objets nécessaires
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

		// Configuration des paramètres internes pour le test
		InternalTestHelper.setInternalUserNumber(100000);

		// Initialisation du service TourGuideService
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		// Récupération de la liste de tous les utilisateurs
		List<User> allUsers = tourGuideService.getAllUsers();

		// Initialisation du chronomètre
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// Suivi de la localisation de tous les utilisateurs en parallèle et attente de la fin
		tourGuideService.trackAllUserLocation(allUsers).join();

		// Arrêt du chronomètre et du suivi de localisation
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		// Affichage du temps écoulé pour le test
		System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		// Vérification que le temps écoulé est inférieur ou égal à 15 minutes
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

		// Calcul des récompenses pour chaque utilisateur
		allUsers.forEach(rewardsService::calculateRewards);

		// Arrêt du service d'exécution des tâches de calcul des récompenses
		rewardsService.getExecutorService().shutdown();

		// Attente de la fin de toutes les tâches pendant 19 minutes
		boolean allTasksIsFinished = rewardsService.getExecutorService().awaitTermination(19, TimeUnit.MINUTES);

		// Affichage de l'état de toutes les tâches
		System.out.println("Tâches finies: " + allTasksIsFinished);

		// Vérification que chaque utilisateur a reçu des récompenses
		for(User user : allUsers) {
			assertTrue(user.getUserRewards().size() > 0);
		}

		// Arrêt du chronomètre et du suivi de localisation
		stopWatch.stop();
		tourGuideService.tracker.stopTracking();

		// Affichage du temps écoulé pour le test
		System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

		// Vérification que le temps écoulé est inférieur ou égal à 20 minutes
		assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
	}


}
