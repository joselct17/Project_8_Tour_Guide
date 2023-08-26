package tourGuide.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.model.user.User;
import tourGuide.model.user.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	private final ExecutorService executorService = Executors.newFixedThreadPool(1000);

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}
	
	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}
	
	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {

		executorService.execute(() -> {
			Iterable<VisitedLocation> userLocations = new ArrayList<>(user.getVisitedLocations());
			List<Attraction>          attractions   = gpsUtil.getAttractions();

			for (VisitedLocation visitedLocation : userLocations) {

				for (Attraction attraction : attractions) {

					if (user.getUserRewards()
							.stream()
							.noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {

						if (this.nearAttraction(visitedLocation, attraction)) {

							user.addUserReward(new UserReward(visitedLocation, attraction,
									getRewardPoints(attraction, user)
							));
						}
					}
				}
			}
		});
	}

	public void calculateRewardss(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();
		List<UserReward> newRewards = new ArrayList<>(); // Create a list for new rewards

		for (VisitedLocation visitedLocation : userLocations) {
			for (Attraction attraction : attractions) {
				if (user.getUserRewards().stream().noneMatch(r -> r.attraction.attractionName.equals(attraction.attractionName))) {
					if (nearAttraction(visitedLocation, attraction)) {
						newRewards.add(new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					}
				}
			}
		}

		// After both loops are complete, add the new rewards to the user's rewards list
		user.getUserRewards().addAll(newRewards);
	}

	
	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}
	
	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}
	
	private int getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}
	
	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

	/**
	 * Renvoie l'instance de ExecutorService associée à cet objet.
	 *
	 * @return L'ExecutorService associée.
	 */
	public ExecutorService getExecutorService() {
		return this.executorService;
	}

	/**
	 * Attend que toutes les tâches en cours de l'ExecutorService soient terminées.
	 * Après l'appel à cette méthode, l'ExecutorService sera arrêté pour empêcher
	 * la soumission de nouvelles tâches.
	 * Cette méthode bloque l'exécution jusqu'à ce que toutes les tâches soient terminées.
	 */
	public void waitAllWorkCompleted() {
		// Arrête l'ExecutorService pour empêcher la soumission de nouvelles tâches.
		this.getExecutorService().shutdown();

		// Boucle tant que des tâches sont en cours d'exécution.
		while (true) {
			// Vérifie si toutes les tâches ont été terminées.
			boolean isFinish = this.getExecutorService().isTerminated();

			// Si toutes les tâches sont terminées, sort de la boucle.
			if (isFinish) {
				return;
			}
		}
	}


}
