package org.sc.data;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.sc.common.config.DataSource;
import org.sc.common.rest.controller.Position;
import org.sc.common.rest.controller.Trail;
import org.sc.common.rest.controller.TrailPreview;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Repository
public class TrailDAO {

    public static final String $_NEAR_OPERATOR = "$near";
    public static final String NEAR_OPERATOR = "near";
    public static final String LIMIT = "$limit";
    public static final String RESOLVED_COORDINATES = "coordinates";
    static final String $_MAX_M_DISTANCE_FILTER = "$maxDistance";
    static final String $_MIN_DISTANCE_FILTER = "$minDistance";
    public static final String $_GEO_NEAR_OPERATOR = "$geoNear";
    public static final String DISTANCE_FIELD = "distanceField";
    public static final String KEY_FIELD = "key";
    public static final String INCLUDE_LOCS_FIELD = "includeLocs";
    public static final String MAX_DISTANCE_M = "maxDistance";
    public static final String SPHERICAL_FIELD = "spherical";
    public static final String UNIQUE_DOCS_FIELD = "uniqueDocs";

    private static final String RESOLVED_START_POS_COORDINATE = Trail.START_POS + "." + Position.COORDINATES;

    // Max number of documents output per request
    public static final int RESULT_LIMIT = 150;
    public static final int RESULT_LIMIT_ONE = 1;

    private final MongoCollection<Document> collection;

    private final Mapper<Trail> trailMapper;
    private final Mapper<TrailPreview> trailPreviewMapper;

    @Autowired
    public TrailDAO(final DataSource dataSource,
                    final TrailMapper trailMapper,
                    final TrailPreviewMapper trailPreviewMapper) {
        this.collection = dataSource.getDB().getCollection(Trail.COLLECTION_NAME);
        this.trailMapper = trailMapper;
        this.trailPreviewMapper = trailPreviewMapper;
    }

    public Trail getTrailByCodeAndPostcodeCountry(final String trailCode,
                                                  final String country) {
        final FindIterable<Document> documents = collection.find(
                new Document(Trail.COUNTRY, country)
                        .append(Trail.CODE, trailCode))
                .limit(RESULT_LIMIT_ONE);
        return toTrailsList(documents).stream().findFirst().orElseThrow(IllegalArgumentException::new);
    }

    public List<Trail> getTrailsByStartPosMetricDistance(final double longitude,
                                                         final double latitude,
                                                         final double meters,
                                                         final int limit) {
        final FindIterable<Document> documents = collection.find(
                new Document(RESOLVED_START_POS_COORDINATE,
                        new Document($_NEAR_OPERATOR,
                                new Document(RESOLVED_COORDINATES, Arrays.asList(longitude, latitude)
                                )
                        ).append($_MIN_DISTANCE_FILTER, 0).append($_MAX_M_DISTANCE_FILTER, meters))).limit(limit);
        return toTrailsList(documents);
    }

    @NotNull
    public List<Trail> trailsByPointDistance(double longitude, double latitude, double meters, int limit) {
        final AggregateIterable<Document> aggregate = collection.aggregate(Arrays.asList(new Document($_GEO_NEAR_OPERATOR,
                        new Document(NEAR_OPERATOR, new Document("type", "Point").append("coordinates", Arrays.asList(longitude, latitude)))
                                .append(DISTANCE_FIELD, "distanceToIt")
                                .append(KEY_FIELD, "geoPoints.coordinates")
                                .append(INCLUDE_LOCS_FIELD, "closestLocation")
                                .append(MAX_DISTANCE_M, meters)
                                .append(SPHERICAL_FIELD, "true")
                                .append(UNIQUE_DOCS_FIELD, "true")),
                new Document(LIMIT, limit)
        ));
        return toTrailsList(aggregate);
    }

    @NotNull
    public List<Trail> getTrails(boolean isLight) {
        if(isLight){
            // TODO: filter the points at db level
            return executeQueryAndGetResult(new Document());
        }
        return executeQueryAndGetResult(new Document());
    }

    @NotNull
    public List<Trail> getTrailByCode(@NotNull String code) {
        return toTrailsList(collection.find(new Document("code", code)));
    }

    public List<Trail> executeQueryAndGetResult(final Document doc) {
        return toTrailsList(collection.find(doc).limit(RESULT_LIMIT));
    }

    public boolean delete(final String code) {
        return collection.deleteOne(new Document(Trail.CODE, code)).getDeletedCount() > 0;
    }

    public void upsert(final Trail trailRequest) {
        final Document trail = trailMapper.mapToDocument(trailRequest);
        collection.replaceOne(new Document(Trail.CODE, trailRequest.getCode()),
                trail, new ReplaceOptions().upsert(true));
    }

    @NotNull
    public List<TrailPreview> getAllTrailPreviews() {
        return toTrailsPreviewList(collection.find().projection(projectPreview()));
    }

    @NotNull
    public List<TrailPreview> trailPreviewByCode(@NotNull String code) {
        return toTrailsPreviewList(collection.find(new Document(Trail.CODE, code)).projection(projectPreview()));
    }

    private List<TrailPreview> toTrailsPreviewList(FindIterable<Document> documents) {
        return StreamSupport.stream(documents.spliterator(), false).map(trailPreviewMapper::mapToObject).collect(toList());
    }

    private Document projectPreview() {
        return new Document(Trail.CODE, 1).append(Trail.START_POS, 1).append(Trail.FINAL_POS, 1).append(Trail.CLASSIFICATION, 1)
                .append(Trail.LAST_UPDATE_DATE, 1);
    }

    @NotNull
    private List<Trail> toTrailsList(Iterable<Document> documents) {
        return StreamSupport.stream(documents.spliterator(), false).map(trailMapper::mapToObject).collect(toList());
    }

}