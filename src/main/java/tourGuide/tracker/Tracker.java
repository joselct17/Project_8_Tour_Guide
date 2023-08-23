package tourGuide.tracker;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tourGuide.service.TourGuideService;
import tourGuide.model.user.User;

public class Tracker extends Thread {
	private Logger logger = LoggerFactory.getLogger(Tracker.class);
	private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final TourGuideService tourGuideService;
	private boolean stop = false;

	public Tracker(TourGuideService tourGuideService) {
		this.tourGuideService = tourGuideService;
		
		executorService.submit(this);
	}
	
	/**
	 * Assures to shut down the Tracker thread
	 */
	public void stopTracking() {
		stop = true;
		executorService.shutdownNow();
	}



	@Override
	public void run() {
		StopWatch stopWatch = new StopWatch();
		while (true) {
			// Vérifie si le thread en cours a été interrompu ou si l'arrêt a été demandé
			if (Thread.currentThread().isInterrupted() || stop) {
				logger.debug("Tracker stopping");
				break;
			}

			// Récupération de la liste des utilisateurs depuis le service TourGuideService
			List<User> users = tourGuideService.getAllUsers();
			logger.debug("Begin Tracker. Tracking " + users.size() + " users.");

			// Création de futures pour le suivi de localisation de chaque utilisateur en parallèle
			List<CompletableFuture<Void>> futures = users.stream()
					.map(u -> CompletableFuture.runAsync(() -> tourGuideService.trackUserLocation(u)))
					.collect(Collectors.toList());

			// Création d'un CompletableFuture qui attend la fin de tous les futures
			CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

			try {
				stopWatch.start();
				allOf.get(); // Attend la fin de tous les CompletableFuture
				stopWatch.stop();

				// Affichage du temps écoulé pour le suivi de localisation
				logger.debug("Tracker Time Elapsed: " + TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()) + " seconds.");
				stopWatch.reset();

				// Attente pour le prochain cycle de suivi en respectant l'intervalle de sondage
				logger.debug("Tracker sleeping");
				TimeUnit.SECONDS.sleep(trackingPollingInterval);
			} catch (InterruptedException | ExecutionException e) {
				break;
			}
		}
	}

}
