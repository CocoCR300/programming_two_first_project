package com.una.programming_two_first_project.data_store;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.una.programming_two_first_project.data_store.annotation.ForeignKey;
import com.una.programming_two_first_project.data_store.annotation.InverseProperty;
import com.una.programming_two_first_project.data_store.annotation.PrimaryKey;
import com.una.programming_two_first_project.model.*;
import com.una.programming_two_first_project.util.LocalDateAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.una.programming_two_first_project.model.Tuple.tuple;

public class SimpleDataStore implements DataStore
{
    private final Executor singleThreadExecutor;
//    private final Map<String, Collaborator> collaborators;
//    private final Map<String, Department> departments;
//    private final Map<String, Project> projects;
//    private final Map<String, Sprint> sprints;
//    private final Map<String, Task> tasks;
    private final Map<String, ModelInfo<? extends Model>> modelInfoByKey;
    private final Map<String, Map<String,? extends Model>> entitiesByName;
    private final String applicationDataFolderPath;

    @Inject
    public SimpleDataStore(String applicationDataFolderPath) {
        this.applicationDataFolderPath = applicationDataFolderPath;

//        collaborators = new HashMap<>();
//        departments = new HashMap<>();
//        projects = new HashMap<>();
//        sprints = new HashMap<>();
//        tasks = new HashMap<>();
        // The map returned by Map.of() is unmodifiable, so we have to create a new map to be able to work on it
        entitiesByName = new HashMap<>();
        modelInfoByKey = new HashMap<>();

        for (Class<? extends Model> modelClass : new Class[] { Collaborator.class, Department.class, Project.class, Sprint.class, Task.class }) {
            String modelKey = getModelKey(modelClass);
            entitiesByName.put(modelKey, null);

            Field primaryKeyField = null;
            List<Field> foreignKeyFields = new ArrayList<>();
            List<Field> inversePropertyFields = new ArrayList<>();
            for (Field modelField : modelClass.getFields()) {
                ForeignKey foreignKey = modelField.getAnnotation(ForeignKey.class);
                PrimaryKey primaryKey = modelField.getAnnotation(PrimaryKey.class);
                InverseProperty inverseProperty = modelField.getAnnotation(InverseProperty.class);

                if (primaryKey != null) {
                    primaryKeyField = modelField;
                } else if (foreignKey != null) {
                    foreignKeyFields.add(modelField);
                } else if (inverseProperty != null) {
                    inversePropertyFields.add(modelField);
                }
            }
            assert primaryKeyField != null;

            modelInfoByKey.put(modelKey, new ModelInfo<>(modelClass, primaryKeyField, foreignKeyFields, inversePropertyFields));
        }

        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

//    private boolean areEntitiesLoaded(String modelKey) {
//        return entitiesByName.unwrap(modelKey).isPresent();
//    }

    private boolean changesMadeTo(String modelKey) {
        return modelInfoByKey.get(modelKey).changeCounter > 0;
    }

    private File getModelFile(String modelKey) {
        return Path.of(applicationDataFolderPath, modelKey + ".json").toFile();
    }

    private Gson createGson() {
        return new GsonBuilder()
                .setLenient()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create();
    }

    private <T extends Model> Result<Optional<T>, String> get(String modelKey, String id) {
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.map(m -> {
            T entity = m.get(id);
            return Optional.ofNullable(entity);
        });
    }

    private <T extends Model> Result<Map<String, T>, String> getAll(String modelKey) {
        if (!entitiesByName.containsKey(modelKey)) {
            return Result.err(DataStore.MODEL_NOT_FOUND);
        }

        Map<String, T> map = (Map<String, T>) entitiesByName.get(modelKey);
        if (map == null) {
            map = loadEntities(modelKey);
        }
        return Result.ok(map);
    }

    private String getModelKey(Class<? extends Model> modelClass) {
        return modelClass.getSimpleName().toLowerCase();
    }

    private <T extends Model> Map<String, T> loadEntities(String modelKey) {
        Gson gson = createGson();
        Map<String, T> entityMap = new HashMap<>();
        entitiesByName.put(modelKey, entityMap);

        File entitiesFile = getModelFile(modelKey);
        if (!entitiesFile.exists()) {
            return entityMap;
        }

        try (FileReader fileReader = new FileReader(entitiesFile); BufferedReader bufferedReader = new BufferedReader(fileReader);
             JsonReader jsonReader = gson.newJsonReader(bufferedReader)) {
            jsonReader.setLenient(true);
            Class<? extends Model> modelClass = modelInfoByKey.get(modelKey).modelClass;
            Constructor<? extends Model> modelConstructor = modelClass.getConstructor();
            List<Tuple<Field, Field>> relationFields = new ArrayList<>();
            Map<String, Field> modelFields = Arrays.stream(modelClass.getFields())
                    .peek(field -> {
                        field.setAccessible(true);

                        try {
                            ForeignKey foreignKey;
                            InverseProperty inverseProperty;
                            if ((foreignKey = field.getAnnotation(ForeignKey.class)) != null) {
                                Field relationField = modelClass.getField(foreignKey.relationFieldName());

                                relationFields.add(tuple(relationField, field));
                            } else if ((inverseProperty = field.getAnnotation(InverseProperty.class)) != null) {
                                String relatedModelKey = getModelKey(inverseProperty.relationModelClass());
                                Class<? extends Model> relatedModelClass = modelInfoByKey.get(relatedModelKey).modelClass;
                                Field relatedModelRelationIdField = relatedModelClass.getField(inverseProperty.relationModelRelationIdFieldName());
                                relatedModelRelationIdField.setAccessible(true);

                                relationFields.add(tuple(field, relatedModelRelationIdField));
                            }
                        } catch (NoSuchFieldException ex) {
                            // TODO
                        }
                    })
                    .collect(Collectors.toMap(Field::getName, Function.identity()));

            jsonReader.beginArray();

            // TODO: I think there is a high time complexity here, two loops, one that reads from the JSON entitiesFile
            // and another that relates entities together, doesn't look so great, but it will work for now...
            while (jsonReader.hasNext()) {
                T newEntity = (T) modelConstructor.newInstance();
                jsonReader.beginObject();

                while (jsonReader.hasNext()) {
                    String attributeName = jsonReader.nextName();
                    String typeName = jsonReader.peek().toString();

                    var field = modelFields.get(attributeName);

                    if (typeName.equals("ARRAY")) {
                        jsonReader.skipValue();
                        continue;
                    }

                    TypeAdapter<?> adapter = gson.getAdapter(field.getType());
                    Object value = adapter.read(jsonReader);

//                    switch (typeName) {
//                        case "ARRAY":
//                            jsonReader.skipValue();
//                            break;
//                        case "BOOLEAN":
//                            value = jsonReader.nextBoolean();
//                            break;
//
//                        case "INTEGER":
//                            value = jsonReader.nextInt();
//                            break;
//
//                        case "STRING":
//                            value = jsonReader.nextString();
//
//                            if (value == null) {
//                                value = "";
//                            }
//                            break;
//                    }

                    if (value != null) {
                        field.set(newEntity, value);
                    }
                }
                jsonReader.endObject();
                entityMap.put(newEntity.getId(), newEntity);
            }

            for (Tuple<Field, Field> relationAndRelationIdFields : relationFields) {
                Collection<? extends Model> entitiesOfRelationFieldType = null;
                Field relationField = relationAndRelationIdFields.x();
                relationField.setAccessible(true);
                Field relationIdField = relationAndRelationIdFields.y();
                String relatedModelKey = relationField.getName();

                for (Model newEntity : entityMap.values()) {
                    InverseProperty inverseProperty;
                    if ((inverseProperty = relationField.getAnnotation(InverseProperty.class)) != null) { // Is collection type
                        Class<? extends Model> relationModelClass = inverseProperty.relationModelClass();
                        if (entitiesOfRelationFieldType == null) {
                            entitiesOfRelationFieldType = getAll(getModelKey(relationModelClass)).unwrap().values();
                        }
                        List<Model> relatedEntities = new ArrayList<>();

                        for (Model entity : entitiesOfRelationFieldType) {
                            if (newEntity.getId().equals(relationIdField.get(entity))) {
                                relatedEntities.add(entity);
                            }
                        }

                        relationField.set(newEntity, relatedEntities);
                    } else if (relationIdField.getAnnotation(ForeignKey.class) != null) {
                        String relatedEntityId = (String) relationIdField.get(newEntity);
                        if (!relatedEntityId.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Result<Optional<Model>, String> result = get(getModelKey((Class<? extends Model>) relationField.getType()), relatedEntityId);

                            result.inspect(o -> {
                                // TODO: Why use Optional then...
                                Model value = o.orElse(null);
                                // If this "other" value is used, then something unexpected happened,
                                // the file was modified by hand or anything else
                                try {
                                    relationField.set(newEntity, value);
                                } catch (IllegalAccessException ex) {
                                    // TODO
                                }
                            });
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // TODO
        }

        return entityMap;

//        try {
//
//            CompletableFuture.runAsync(() -> {
//
//
//            }, singleThreadExecutor).unwrap();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
    }

    private <T extends Model> Result<T, String> checkEntityRelations(@NotNull Class<T> modelClass, @Nullable T existingEntity,
                                                                     @Nullable T newEntity, boolean force) {
        ModelInfo modelInfo = modelInfoByKey.get(getModelKey(modelClass));
        // TODO: Not using this Stream as an advantage
        Field[] modelRelationFields = (Field[]) Stream.concat(modelInfo.foreignKeyFields.stream(), modelInfo.inversePropertyFields.stream()).toArray(Field[]::new);
        for (Field field : modelRelationFields) {
            try {
            // https://learn.microsoft.com/en-us/dotnet/api/microsoft.entityframeworkcore.dbset-1.update?view=efcore-7.0#microsoft-entityframeworkcore-dbset-1-update(-0)
            // A recursive search of the navigation properties will be performed to find reachable entities that
            // are not already being tracked by the context. All entities found will be tracked by the context.

                ForeignKey foreignKey;
                InverseProperty inverseProperty;
                if ((inverseProperty = field.getAnnotation(InverseProperty.class)) != null) {
                    field.setAccessible(true);

                    String fieldName = field.getName();
                    List<? extends Model> oldRelatedEntities = existingEntity != null ? (List<? extends Model>) field.get(existingEntity) : List.of();
                    List<? extends Model> newRelatedEntities = newEntity != null ? (List<? extends Model>) field.get(newEntity) : List.of();
                    Class<? extends Model> relatedModelClass = inverseProperty.relationModelClass();
                    Field relatedModelRelationField = relatedModelClass.getField(inverseProperty.relationModelRelationFieldName());
                    Field relatedModelRelationIdField = relatedModelClass.getField(inverseProperty.relationModelRelationIdFieldName());
                    relatedModelRelationField.setAccessible(true);
                    relatedModelRelationIdField.setAccessible(true);

                    // TODO: A slow operation. Use HashSet?
                    List<? extends Model> allEntitiesRelated = Stream
                            .concat(oldRelatedEntities.stream(), newRelatedEntities.stream())
                            .distinct()
                            .toList();
                    int changes = 0;
                    for (Model relatedEntity : allEntitiesRelated) {
                        String relationId = (String) relatedModelRelationIdField.get(relatedEntity);

                        // TODO: This won't work just fine with Sprint.id
                        if (!newRelatedEntities.contains(relatedEntity)) {
                            // Old relation that should be removed
                            relatedModelRelationField.set(relatedEntity, null);
                            relatedModelRelationIdField.set(relatedEntity, "");
                        } else if (!oldRelatedEntities.contains(relatedEntity)) {
                            // New relation that needs to be checked, after that, it can be updated
                            if (!relationId.isEmpty() && !force) {
                                resetEntitiesMapChangeFlag(getModelKey(relatedModelClass));
                                return Result.err(ENTITY_ALREADY_RELATED);
                            } else {
                                // We would need to roll back these changes, but the SimpleDataStore is more adapted to
                                // the behavior of this application (a single operation at a time), so it shouldn't be needed
                                relatedModelRelationField.set(relatedEntity, newEntity);
                                relatedModelRelationIdField.set(relatedEntity, newEntity.getId());
                                changes++;
                            }
                        }
                    }

                    String relatedModelName = fieldName.substring(0, fieldName.length() - 1);
                    incrementEntitiesMapChangeCounter(relatedModelName, changes);

                } else if ((foreignKey = field.getAnnotation(ForeignKey.class)) != null) {
                    Class<? extends Model> relatedModelClass = foreignKey.relationModelClass();
                    ModelInfo relatedModelInfo = modelInfoByKey.get(getModelKey(relatedModelClass));
                    String relatedModelKey = getModelKey(relatedModelClass);

                    for (Field inversePropertyFieldOnRelatedEntity : (List<Field>) relatedModelInfo.inversePropertyFields) {
                        if (inversePropertyFieldOnRelatedEntity.getAnnotation(InverseProperty.class).relationModelClass() == modelClass) {
                            // The models just have collection-type fields on the other end of a relation given
                            // by a ForeignKey, we can just assume is a collection-type
                            Field relationField = modelClass.getField(foreignKey.relationFieldName());
                            relationField.setAccessible(true);
                            Model oldRelatedEntity;
                            if (existingEntity != null && (oldRelatedEntity = (Model) relationField.get(existingEntity)) != null) {
                                // Remove "newEntity" from the collection-type relation field of the entity that it was
                                // related to
                                List<Model> oldRelatedEntityRelatedEntities = (List<Model>) inversePropertyFieldOnRelatedEntity.get(oldRelatedEntity);
                                oldRelatedEntityRelatedEntities.remove(newEntity);
                                incrementEntitiesMapChangeCounter(relatedModelKey);
                            }

                            Model newRelatedEntity = (Model) relationField.get(newEntity);
                            if (newRelatedEntity != null) {
                                // Relate "newEntity" to the entity indicated by one of its foreign keys
                                List<Model> newRelatedEntityRelatedEntities = (List<Model>) inversePropertyFieldOnRelatedEntity.get(newRelatedEntity);
                                newRelatedEntityRelatedEntities.add(newEntity);
                                incrementEntitiesMapChangeCounter(relatedModelKey);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // TODO
            }
//                if (Model.class.isAssignableFrom(field.getType())) {
//                    field.setAccessible(true);
//                    try {
//                        Model relatedEntity = (Model) field.get(newEntity);
//                        Class<? extends Model> relatedEntityClass = (Class<? extends Model>) field.getType();
//                        var possibleRelatedEntity = get(relatedEntityClass, relatedEntity.getId()).unwrap();
//                        if (possibleRelatedEntity.isEmpty()) {
//                            update(relatedEntity);
//                        }
//                    } catch (IllegalAccessException e) {
//                        // TODO
//                    }
//                }
        }

        return Result.ok(newEntity);
    }

    private <T extends Model> void assignPrimaryKey(T newEntity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(entityClass);
        Field primaryKeyField = modelInfoByKey.get(modelKey).primaryKeyField;
        PrimaryKey primaryKeyAnnotation;
        if ((primaryKeyAnnotation = primaryKeyField.getAnnotation(PrimaryKey.class)).autogenerate()) {
            primaryKeyField.setAccessible(true);

            try {
                if (primaryKeyField.getType().equals(String.class)) {
                    primaryKeyField.set(newEntity, UUID.randomUUID().toString().toUpperCase());
                } else {
                    Collection<T> entities = (Collection<T>) entitiesByName.get(modelKey).values();
                    Number highestPrimaryKey = 0;
                    for (T entity : entities) {
                        Number currentPrimaryKey = (Number) primaryKeyField.get(entity);

                        if (Objects.compare(highestPrimaryKey, currentPrimaryKey, Comparator.comparingLong(Number::longValue)) == -1) {
                            highestPrimaryKey = currentPrimaryKey;
                        }
                    }

                    // TODO: Overflow?
                    Number primaryKey;
                    if (int.class.isAssignableFrom(primaryKeyField.getType())) {
                        primaryKey = highestPrimaryKey.intValue() + 1;
                    } else if (short.class.isAssignableFrom(primaryKeyField.getType())) {
                        primaryKey = (short) (highestPrimaryKey.shortValue() + 1);
                    } else {
                        primaryKey = highestPrimaryKey.longValue() + 1;
                    }

                    primaryKeyField.set(newEntity, primaryKey);
                }
            } catch (IllegalAccessException e) {
                // TODO
            }
        } else {
            try {
                String[] composerAttributeNames = primaryKeyAnnotation.composerAttributeNames();
                String[] composerAttributeValues = new String[composerAttributeNames.length];

                for (int i = 0; i < composerAttributeNames.length; ++i) {

                    Field composerField = entityClass.getField(composerAttributeNames[i]);
                    composerAttributeValues[i] = composerField.get(newEntity).toString();

                }
                Method composerMethod = null;
                Method[] modelClassMethods = entityClass.getMethods();

                for (Method method : modelClassMethods) {
                    if (method.getName().equals(primaryKeyAnnotation.composerMethodName())) {
                        composerMethod = method;
                    }
                }

                assert composerMethod != null;
                composerMethod.invoke(null, (Object[]) composerAttributeValues);
            } catch (Exception ex) {
                // TODO
            }
        }
    }

    private void incrementEntitiesMapChangeCounter(String modelKey) {
        incrementEntitiesMapChangeCounter(modelKey, 1);
    }

    private void incrementEntitiesMapChangeCounter(String modelKey, int increment) {
        ModelInfo modelInfo = modelInfoByKey.get(modelKey);
        modelInfo.changeCounter += increment;
    }

    private int resetEntitiesMapsChangeFlag() {
        int allChangesCount = 0;
        for (ModelInfo modelInfo : modelInfoByKey.values()) {
            allChangesCount += modelInfo.changeCounter;
            modelInfo.changeCounter = 0;
        }

        return allChangesCount;
    }

    private int resetEntitiesMapChangeFlag(String modelKey) {
        ModelInfo modelInfo = modelInfoByKey.get(modelKey);
        int changeCounter = modelInfo.changeCounter;
        modelInfo.changeCounter = 0;

        return changeCounter;
    }

    @Override
    public Optional<Class<? extends Model>> getClassFromSimpleName(String modelClassSimpleName) {
        var foundInfo = modelInfoByKey.get(modelClassSimpleName);
        return Optional.ofNullable(foundInfo != null ? foundInfo.modelClass : null);
    }

    @Override
    public <T extends Model> Result<T, String> add(T newEntity) {
        Class<T> modelClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(modelClass);
        Result<Map<String, T>, String> result = getAll(modelKey);
        assignPrimaryKey(newEntity);

        return result.andThen(m -> {
            if (!m.containsKey(newEntity.getId())) {
                m.put(newEntity.getId(), newEntity);
                return checkEntityRelations(modelClass, null, newEntity, false).map(e -> {
                    incrementEntitiesMapChangeCounter(modelKey);
                    return e;
                });
            }

            return Result.err(ENTITY_ALREADY_EXISTS);
        });
    }

    @Override
    public <T extends Model> Result<T, String> delete(Class<T> modelClass, String id) {
        String modelKey = getModelKey(modelClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.andThen(m -> {
            if (m.containsKey(id)) {
                T entity = m.get(id);
                checkEntityRelations(modelClass, entity, null, true);
                m.remove(id);
                incrementEntitiesMapChangeCounter(modelKey);
                return Result.ok(entity);
            }

            return Result.err(ENTITY_DOES_NOT_EXIST);
        });
    }

    public <T extends Model> Result<Optional<T>, String> get(Class<T> modelClass, String id) {
        String modelKey = getModelKey(modelClass);
        return get(modelKey, id);
    }

    @Override
    public <T extends Model> Result<Map<String, T>, String> getAll(Class<T> modelClass) {
        String modelKey = getModelKey(modelClass);
        return getAll(modelKey);
    }

    @Override
    public <T extends Model> Result<List<Optional<T>>, String> getMany(Class<T> modelClass, String[] ids) {
        String modelKey = getModelKey(modelClass);
        Result<Map<String, T>, String> entitiesMap = getAll(modelKey);
        return entitiesMap.map(m -> {
            List<Optional<T>> entitiesFound = new ArrayList<>();
            for (String id : ids) {
                T entity = m.get(id);
                entitiesFound.add(Optional.ofNullable(entity));
            }

            return entitiesFound;
        });
    }

    @Override
    public <T extends Model> Result<T, String> update(T newEntity, boolean force) {
        Class<T> modelClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(modelClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.map(m -> {
            String entityId = newEntity.getId();
            T existingEntity = m.get(entityId);

            // TODO: Ignoring Result
            checkEntityRelations(modelClass, existingEntity, newEntity, force);
            if (existingEntity != null) {
                // newEntity and existingEntity could be referencing the same instance, but the replace operation is
                // cheap, so a check for equality is not so necessary
                m.replace(entityId, newEntity);
            } else {
                assignPrimaryKey(newEntity);
                m.put(entityId, newEntity);
            }

            incrementEntitiesMapChangeCounter(modelKey);
            return newEntity;
        });
    }

    @Override
    public <T extends Model> Result<List<T>, String> updateAll(Class<T> modelClass, List<T> newEntities, boolean force) {
        String modelKey = getModelKey(modelClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.map(m -> {
            for (T newEntity : newEntities) {
                String entityId = newEntity.getId();
                T existingEntity = m.get(entityId);

                // TODO: Ignoring Result
                checkEntityRelations(modelClass, existingEntity, newEntity, force);
                if (existingEntity != null) {
                    // newEntity and existingEntity could be referencing the same instance, but the replace operation is
                    // cheap, so a check for equality is not so necessary
                    m.replace(entityId, newEntity);
                } else {
                    assignPrimaryKey(newEntity);
                    m.put(entityId, newEntity);
                }

                incrementEntitiesMapChangeCounter(modelKey, newEntities.size());
            }

            return newEntities;
        });
    }

    @Override
    public Result<Integer, Exception> commitChanges() {
        Gson gson = createGson();

        for (Map.Entry<String, Map<String, ? extends Model>> entry : entitiesByName.entrySet()) {
            if (entry.getValue() != null && changesMadeTo(entry.getKey())) {
                Map<String, ? extends Model> map = entry.getValue();
                String modelKey = entry.getKey();
                Object[] entities = map.values().toArray();
                File entitiesFile = getModelFile(modelKey);
                try (FileWriter fileWriter = new FileWriter(entitiesFile);
                     BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                     JsonWriter jsonWriter = gson.newJsonWriter(bufferedWriter)) {
                    gson.toJson(entities, modelInfoByKey.get(modelKey).modelClass.arrayType(), jsonWriter);
                } catch (IOException ex) {
                    return Result.err(ex);
                }
            }
        }

        int allChangesCount = resetEntitiesMapsChangeFlag();
        return Result.ok(allChangesCount);
    }

//    public <T extends Model> Result<T, String> unwrap(Class<T> modelClass, String id) {
//        Result<Map<String, T>, String> result = getAll(modelClass);
//        return result.andThen(m -> {
//            T model = m.unwrap(id);
//
//            if (model != null) {
//                return Result.ok(model);
//            }
//
//            return Result.err(DataStore.ENTITY_NOT_FOUND);
//        });
//    }

    private class ModelInfo<T extends Model>
    {
        public final Class<T> modelClass;
        public final Field primaryKeyField;
        public final List<Field> foreignKeyFields;
        public final List<Field> inversePropertyFields;
        public int changeCounter;

        public ModelInfo(Class<T> modelClass, Field primaryKeyField, List<Field> foreignKeyFields, List<Field> inversePropertyFields) {
            this.modelClass = modelClass;
            this.primaryKeyField = primaryKeyField;
            this.foreignKeyFields = foreignKeyFields;
            this.inversePropertyFields = inversePropertyFields;
        }
    }
}
