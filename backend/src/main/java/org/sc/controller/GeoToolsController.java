package org.sc.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.sc.adapter.AltitudeServiceAdapter;
import org.sc.common.rest.CoordinatesDto;
import org.sc.data.validator.GeneralValidator;
import org.sc.manager.GeoToolManager;
import org.sc.processor.DistanceProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(GeoToolsController.PREFIX)
public class GeoToolsController {

    public final static String PREFIX = "/geo-tool";

    private final GeneralValidator generalValidator;
    private final GeoToolManager geoToolManager;


    @Autowired
    public GeoToolsController(final GeneralValidator generalValidator,
                              final GeoToolManager geoToolManager) {
        this.generalValidator = generalValidator;
        this.geoToolManager = geoToolManager;
    }

    @Operation(summary = "Find a point elevation by lat-long")
    @GetMapping("/altitude")
    public CoordinatesDto geoLocateTrail(@RequestParam double latitude,
                                         @RequestParam double longitude) {
        final Set<String> errors = generalValidator.validate(new CoordinatesDto(latitude, longitude));
        if(!errors.isEmpty()) {
            return new CoordinatesDto();
        }
        final double altitudeByLongLat = geoToolManager.getAltitudeByLongLat(latitude, longitude);
        return new CoordinatesDto(latitude, longitude, altitudeByLongLat);
    }

    @Operation(summary = "Find coordinates distance")
    @PostMapping("/distance")
    public Double radialDistance(@RequestBody List<CoordinatesDto> coordinatesDtoList) {
        final Set<String> errors = coordinatesDtoList.stream().map(generalValidator::validate)
                .flatMap(Collection::stream).collect(Collectors.toSet());
        if(!errors.isEmpty()) {
            return (double) 0;
        }
        return geoToolManager.getDistanceBetweenCoordinates(coordinatesDtoList);
    }
}
