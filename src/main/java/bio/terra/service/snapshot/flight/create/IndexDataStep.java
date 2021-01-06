package bio.terra.service.snapshot.flight.create;

import bio.terra.common.exception.PdaoException;
import bio.terra.model.SnapshotRequestModel;
import bio.terra.service.iam.AuthenticatedUserRequest;
import bio.terra.service.snapshot.Snapshot;
import bio.terra.service.snapshot.SnapshotService;
import bio.terra.service.snapshot.SnapshotTable;
import bio.terra.service.tabulardata.google.BigQueryPdao;
import bio.terra.stairway.FlightContext;
import bio.terra.stairway.Step;
import bio.terra.stairway.StepResult;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.PutMappingRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class IndexDataStep implements Step {
    private final SnapshotService snapshotService;
    private final SnapshotRequestModel snapshotRequestModel;
    private final BigQueryPdao bigQueryPdao;
    private final AuthenticatedUserRequest userReq;
    private static final Logger logger = LoggerFactory.getLogger(IndexDataStep.class);

    public IndexDataStep(SnapshotService snapshotService,
                         SnapshotRequestModel snapshotRequestModel,
                         BigQueryPdao bigQueryPdao,
                         AuthenticatedUserRequest userReq) {
        this.snapshotService = snapshotService;
        this.snapshotRequestModel = snapshotRequestModel;
        this.bigQueryPdao = bigQueryPdao;
        this.userReq = userReq;
    }

    @Override
    public StepResult doStep(FlightContext context) throws InterruptedException {
        // Create a client connection to the ES index. This should be moved to a bean and use config for connection parameters
        final RestClientBuilder builder = RestClient.builder( new HttpHost("localhost", 9200));
        try (RestHighLevelClient client = new RestHighLevelClient(builder)) {

            final Snapshot snapshot = snapshotService.retrieveByName(snapshotRequestModel.getName());
            final String indexName = String.format("idx-%s", snapshot.getId());

            // Create the ES index
            createIndex(client, indexName);

            // Create field mappings
            snapshot.getTables().forEach(t -> createMapping(client, indexName, t));

            // Load the data into the elasticsearch index
            snapshot.getTables().forEach(t -> {
                final BulkRequest request = new BulkRequest();
                final String typeName = t.getName();
                final List<Map<String, Object>> values;
                try {
                    values = bigQueryPdao.getSnapshotTableData(snapshot, t.getName());
                } catch (final InterruptedException e) {
                    throw new PdaoException("Error retrieving data", e);
                }
                values
                    .stream()
                    .map(v -> {
                        Map<String, Object> typedSource = new HashMap<>(v);
                        // Explicitly add the "type" field which is the table name
                        typedSource.put("type", typeName);
                        // Namespace the fields to avoid collisions across tables
                        return typedSource.entrySet().stream().collect(Collectors.toMap(
                            e -> e.getKey().equals("datarepo_row_id") || e.getKey().equals("type")
                                ? e.getKey()
                                : String.format("%s.%s", t.getName(), e.getKey()),
                            Map.Entry::getValue
                        ));
                    })
                    .forEach(v -> request.add(new IndexRequest(indexName)
                        .id(v.get("datarepo_row_id").toString())
                        .source(v, XContentType.JSON)
                    ));

                try {
                    if (!values.isEmpty()) {
                        client.bulk(request, RequestOptions.DEFAULT);
                    }
                } catch (final IOException e) {
                    throw new PdaoException("Error indexing data", e);
                }
            });
        } catch (IOException e) {
            throw new PdaoException("Error creating ES client", e);
        }

        return StepResult.getStepResultSuccess();
    }

    private void createIndex(final RestHighLevelClient client, final String indexName) {
        try {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName)
                .settings(Settings.builder()
                    .put("index.number_of_shards", 2));
            CreateIndexResponse response = null;
            response = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            logger.info("Created index {}", response);
        } catch (IOException e) {
            // Convert to real error
            throw new PdaoException("Error creating index", e);
        }
    }

    private void createMapping(final RestHighLevelClient client,
                               final String indexName,
                               final SnapshotTable snapshotTable) {
        try {
            final PutMappingRequest request = new PutMappingRequest(indexName);
            final Map<String, Object> properties = snapshotTable.getColumns().stream().collect(Collectors.toMap(
                c -> String.format("%s.%s", snapshotTable.getName(), c.getName()),
                c -> {
                    final Map<String, Object> mapping = new HashMap<>();
                    if (c.getType() == null || Objects.equals(c.getType().toUpperCase(), "STRING") ||
                        Objects.equals(c.getType().toUpperCase(), "FILE_REF")) {
                        mapping.put("type", "text");
                    } else if (Objects.equals(c.getType().toUpperCase(), "DATE")) {
                        mapping.put("type", "date");
                    } else if (Objects.equals(c.getType().toUpperCase(), "INTEGER")) {
                        mapping.put("type", "integer");
                    } else {
                        throw new PdaoException("Didn't recognize type " + c.getType());
                    }

                    return mapping;
                }
            ));
            // Gets overwritten but it's ok
            properties.put("type", Collections.singletonMap("type", "keyword"));
            properties.put("datarepo_row_id", Collections.singletonMap("type", "keyword"));
            final Map<String, Object> source = new HashMap<>();
            source.put("properties", properties);
            request.source(source);
            client.indices().putMapping(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            // Convert to real error
            throw new PdaoException("Error creating mapping", e);
        }
    }

    @Override
    public StepResult undoStep(FlightContext context) {
        return StepResult.getStepResultSuccess();
    }
}
