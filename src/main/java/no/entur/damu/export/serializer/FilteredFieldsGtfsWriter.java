package no.entur.damu.export.serializer;

import org.onebusaway.csv_entities.schema.EntitySchema;
import org.onebusaway.csv_entities.schema.FieldMapping;
import org.onebusaway.gtfs.serialization.GtfsWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * GTFSWriter that ignores specific GTFS fields.
 */
public class FilteredFieldsGtfsWriter extends GtfsWriter {

    private final Map<Class<?>, Collection<String>> filteredFields;

    public FilteredFieldsGtfsWriter(Map<Class<?>, Collection<String>> filteredFields) {
        this.filteredFields = filteredFields;
    }

    @Override
    public void excludeOptionalAndMissingFields(Class<?> entityType, Iterable<Object> entities) {
        super.excludeOptionalAndMissingFields(entityType, entities);

        filteredFields.forEach((key, value) -> {
            EntitySchema entitySchema = this.getEntitySchemaFactory().getSchema(key);
            removeFields(entitySchema, value);
        });
    }

    private static void removeFields(EntitySchema entitySchema, Collection<String> excludedFields) {
        Iterator<FieldMapping> iterator = entitySchema.getFields().iterator();
        while (iterator.hasNext()) {
            FieldMapping field = iterator.next();
            Collection<String> fieldNames = new ArrayList<>();
            field.getCSVFieldNames(fieldNames);
            String fieldName = fieldNames.stream().findFirst().orElse("");
            if (excludedFields.contains(fieldName)) {
                iterator.remove();
            }
        }
    }

}
