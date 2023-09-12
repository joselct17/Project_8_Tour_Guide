package tourGuide.tracker;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import gpsUtil.GpsUtil;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;

public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);
	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final TourGuideService tourGuideService;
	private final RewardsService rewardsService;
	private final GpsUtil gpsUtil;

	private final ExecutorService executor = Executors.newFixedThreadPool(1000);

	private boolean stop = false;

	public Tracker(TourGuideService tourGuideService, RewardsService rewardsService, GpsUtil gpsUtil) {
		this.tourGuideService = tourGuideService;
		this.rewardsService= rewardsService;
		this.gpsUtil = gpsUtil;

		executorService.submit(this);
	}


	/**
	 * Assures to shut down the Tracker thread
	 */
	public void stopTracking() {
		stop = true;
		executorService.shutdownNow();
	}



//	@Override
//	public void run() {
//		StopWatch stopWatch = new StopWatch();
//		while (true) {
//			// Vérifie si le thread en cours a été interrompu ou si l'arrêt a été demandé
//			if (Thread.currentThread().isInterrupted() || stop) {
//				logger.debug("Tracker stopping");
//				break;
//			}
//
//			// Récupération de la liste des utilisateurs depuis le service TourGuideService
//			List<User> users = tourGuideService.getAllUsers();
//			logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
//
//			// Création de futures pour le suivi de localisation de chaque utilisateur en parallèle
//			List<CompletableFuture<Void>> futures = users.stream()
//					.map(u -> CompletableFuture.runAsync(() -> tourGuideService.trackUserLocation(u)))
//					.collect(Collectors.toList());
//
//			// Création d'un CompletableFuture qui attend la fin de tous les futures
//			CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//
//			try {
//				stopWatch.start();
//				allOf.get(); // Attend la fin de tous les CompletableFuture
//				stopWatch.stop();
//
//				// Affichage du temps écoulé pour le suivi de localisation
//				logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
//				stopWatch.reset();
//
//				// Attente pour le prochain cycle de suivi en respectant l'intervalle de sondage
//				logger.debug("Tracker sleeping");
//				TimeUnit.SECONDS.sleep(trackingPollingInterval);
//			} catch (InterruptedException | ExecutionException e) {
//				break;
//			}
//		}
//	}


	// Redéfinition de la méthode run() pour l'exécution du thread
	@Override
	public void run() {
		// Création d'une instance de StopWatch pour mesurer le temps d'exécution
		StopWatch stopWatch = new StopWatch();

		// Boucle while infinie
		while (true) {
			// Vérification si le thread actuel a été interrompu ou si la variable "stop" est vraie
			if (Thread.currentThread().isInterrupted() || stop) {
				// Affichage d'un message de journalisation indiquant l'arrêt du suivi
				logger.debug("Tracker stopping");
				// Sortie de la boucle while
				break;
			}

			// Récupération de la liste de tous les utilisateurs
			List<User> users = tourGuideService.getAllUsers();

			// Boucle while pour le suivi des utilisateurs
			while (!stop) {
				// Affichage d'un message de journalisation indiquant le début du suivi et le nombre d'utilisateurs
				logger.debug("Begin Tracker. Tracking " + users.size() + " users.");
				// Démarrage du chronomètre
				stopWatch.start();

				// Création d'un tableau de CompletableFuture à partir du traitement asynchrone de chaque utilisateur
				CompletableFuture[] list = users.stream()
						.map(user -> CompletableFuture.runAsync(
								() -> user.addToVisitedLocations(gpsUtil.getUserLocation(user.getUserId())),
								executor
						).thenAccept(location -> rewardsService.calculateRewards(user)))
						.toArray(CompletableFuture[]::new);

				// Création d'un CompletableFuture qui attend la fin de tous les CompletableFuture dans "list"
				CompletableFuture waitEnd = CompletableFuture.allOf(list);

				// Journalisation du nombre de CompletableFuture lancés
				LoggerFactory.getLogger(Tracker.class).debug("{} CompletableFuture launched", list.length);

				// Attendre la fin de l'exécution de tous les CompletableFuture
				waitEnd.join();

				// Arrêt du chronomètre
				stopWatch.stop();

				// Affichage du temps écoulé en secondes pendant le suivi
				logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");

				// Réinitialisation du chronomètre
				stopWatch.reset();

				try {
					// Journalisation de la mise en veille du thread
					logger.debug("Tracker sleeping");
					// Mise en veille du thread pendant "trackingPollingInterval" secondes
					TimeUnit.SECONDS.sleep(trackingPollingInterval);
				} catch (InterruptedException e) {
					// Sortie de la boucle interne si le thread est interrompu pendant la mise en veille
					break;
				}
			}
		}
	}


}
