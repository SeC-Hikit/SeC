package org.sc.manager

import org.sc.common.rest.CoordinatesDto
import org.sc.common.rest.TrailDto
import org.sc.data.entity.Trail
import org.sc.common.rest.TrailPreviewDto
import org.sc.common.rest.UnitOfMeasurement
import org.sc.processor.MetricConverter
import org.sc.data.repository.AccessibilityNotificationDAO
import org.sc.data.repository.MaintenanceDAO
import org.sc.data.repository.TrailDAO
import org.sc.data.TrailDistance
import org.sc.data.dto.TrailMapper
import org.sc.data.dto.TrailPreviewMapper
import org.sc.processor.DistanceProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.logging.Logger
import kotlin.math.roundToInt

@Component
class TrailManager @Autowired constructor(
    private val trailDAO: TrailDAO,
    private val maintenanceDAO: MaintenanceDAO,
    private val accessibilityNotificationDAO: AccessibilityNotificationDAO,
    private val gpxHelper: GpxManager,
    private val trailMapper: TrailMapper,
    private val trailPreviewMapper: TrailPreviewMapper
) {

    private val logger = Logger.getLogger(TrailManager::class.java.name)

    fun get(isLight: Boolean, page: Int, count: Int): List<TrailDto> = trailDAO.getTrails(isLight, page, count)
        .map { trailMapper.trailToTrailDto(it) }

    fun getByCode(code: String, isLight: Boolean): List<TrailDto> = trailDAO.getTrailByCode(code, isLight).map { trailMapper.trailToTrailDto(it) }

    fun delete(code: String, isPurged: Boolean): List<TrailDto> {
        if (isPurged) {
            val deletedMaintenance = maintenanceDAO.deleteByCode(code)
            val deletedAccessibilityNotification = accessibilityNotificationDAO.deleteByCode(code)
            logger.info("Purge deleting trail $code. Maintenance deleted: $deletedMaintenance, deleted notifications: $deletedAccessibilityNotification")
        }
        return trailDAO.delete(code).map { trailMapper.trailToTrailDto(it) }
    }

    fun getPreviews(page: Int, count: Int): List<TrailPreviewDto> =
        trailDAO.getTrailPreviews(page, count).map { trailPreviewMapper.trailPreviewToTrailPreviewDto(it) }

    fun previewByCode(code: String): List<TrailPreviewDto> = trailDAO.trailPreviewByCode(code)
        .map { trailPreviewMapper.trailPreviewToTrailPreviewDto(it) }

    fun save(trail: Trail) {
        trailDAO.upsert(trail)
        gpxHelper.writeTrailToGpx(trail)
    }

    fun getByGeo(coords: CoordinatesDto, distance: Int, unitOfMeasurement: UnitOfMeasurement,
                 isAnyPoint: Boolean, limit: Int): List<TrailDistance> {
        val meters = getMeters(unitOfMeasurement, distance)
        return if (!isAnyPoint) {

            val trailsByStartPosMetricDistance = trailDAO.getTrailsByStartPosMetricDistance(
                    coords.longitude,
                    coords.latitude,
                    meters, limit)
            val trailsDto = trailsByStartPosMetricDistance.map { trailMapper.trailToTrailDto(it) }

            trailsDto.map {
                TrailDistance(
                    DistanceProcessor.distanceBetweenPoints(coords, it.startPos.coordinates).roundToInt(),
                        it.startPos.coordinates, it)
            }
        } else {
            getTrailDistancesWithinRangeAtPoint(coords, distance, unitOfMeasurement, limit)
        }
    }

    fun getTrailDistancesWithinRangeAtPoint(coordinates: CoordinatesDto, distance: Int, unitOfMeasurement: UnitOfMeasurement, limit: Int): List<TrailDistance> {
        val meters = getMeters(unitOfMeasurement, distance)
        val trailsByPointDistance = trailDAO.trailsByPointDistance(
                coordinates.longitude,
                coordinates.latitude,
                meters, limit)

        val trailsDto = trailsByPointDistance.map { trailMapper.trailToTrailDto(it) }

        // for each trail, calculate the distance
        return trailsDto.map {
            val closestCoordinate = getClosestCoordinate(coordinates, it)
            TrailDistance(
                    DistanceProcessor.distanceBetweenPoints(coordinates, closestCoordinate).toInt(),
                    closestCoordinate, it)
        }
    }

    /**
     * Get the trail closest point to a given coordinate
     *
     * @param givenCoordinatesWAltitude the given coordinate
     * @param trail to refer to
     */
    fun getClosestCoordinate(givenCoordinatesWAltitude: CoordinatesDto, trail: TrailDto): CoordinatesDto {
        return trail.coordinates
            .minByOrNull { DistanceProcessor.distanceBetweenPoints(it, givenCoordinatesWAltitude) }!!
    }

    private fun getMeters(unitOfMeasurement: UnitOfMeasurement, distance: Int) =
            if (unitOfMeasurement == UnitOfMeasurement.km) MetricConverter.toM(distance.toDouble()) else distance.toDouble()

}