package com.una.programming_two_first_project.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.una.programming_two_first_project.annotation.ForeignKey;
import com.una.programming_two_first_project.annotation.InverseProperty;
import com.una.programming_two_first_project.annotation.PrimaryKey;
import com.una.programming_two_first_project.model.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final Map<String, Optional<Map<String,? extends Model>>> entitiesByName;
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
            entitiesByName.put(modelKey, Optional.empty());

            Field primaryKeyField = null;
            List<Field> foreignKeyFields = new ArrayList<>();
            for (Field modelField : modelClass.getFields()) {
                ForeignKey foreignKey = modelField.getAnnotation(ForeignKey.class);
                PrimaryKey primaryKey = modelField.getAnnotation(PrimaryKey.class);

                if (primaryKey != null) {
                    primaryKeyField = modelField;
                } else if (foreignKey != null) {
                    foreignKeyFields.add(modelField);
                }
            }
            assert primaryKeyField != null;

            modelInfoByKey.put(modelKey, new ModelInfo<>(modelClass, primaryKeyField, foreignKeyFields));
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
        Optional<Map<String, ? extends Model>> map = entitiesByName.get(modelKey);

        if (map == null) { // TODO: Is this possible at all now that this is private and the other getAll requires a subclass of Model?
            return Result.err(DataStore.MODEL_NOT_FOUND);
        }

        Map<String, T> newMap = (Map<String, T>) map.orElseGet(() -> loadEntities(modelKey));
        return Result.ok(newMap);
    }

    private String getModelKey(Class<? extends Model> modelClass) {
        return modelClass.getSimpleName().toLowerCase();
    }

    private <T extends Model> Map<String, T> loadEntities(String modelKey) {
        Gson gson = createGson();
        Map<String, T> entityMap = new HashMap<>();
        entitiesByName.put(modelKey, Optional.of(entityMap));

        File entitiesFile = getModelFile(modelKey);
//        entitiesFile.createNewFile();
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
                    Object value = null;
                    String attributeName = jsonReader.nextName();
                    String typeName = jsonReader.peek().toString();

                    var field = modelFields.get(attributeName);

                    switch (typeName) {
                        case "ARRAY":
                            jsonReader.skipValue();
                            break;
                        case "BOOLEAN":
                            value = jsonReader.nextBoolean();
                            break;

                        case "INTEGER":
                            value = jsonReader.nextInt();
                            break;

                        case "STRING":
                            value = jsonReader.nextString();

                            if (value == null) {
                                value = "";
                            }
                            break;
                    }

                    field.set(newEntity, value);
                }
                jsonReader.endObject();
                entityMap.put(newEntity.getId(), newEntity);
            }

            for (Tuple<Field, Field> relationAndRelationIdFields : relationFields) {
                Collection<? extends Model> entitiesOfRelationFieldType = null;
                Field relationField = relationAndRelationIdFields.x();
                Field relationIdField = relationAndRelationIdFields.y();
                String relatedModelKey = relationField.getName();
                relatedModelKey = relatedModelKey.substring(0, relatedModelKey.length() - 1);

                for (Model newEntity : entityMap.values()) {
                    if (relationField.getAnnotation(ForeignKey.class) != null) {
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
                    } else { // Is collection type
                        if (entitiesOfRelationFieldType == null) {
                            entitiesOfRelationFieldType = getAll(relatedModelKey).unwrap().values();
                        }
                        List<Model> relatedEntities = new ArrayList<>();

                        for (Model entity : entitiesOfRelationFieldType) {
                            if (newEntity.getId().equals(relationIdField.get(entity))) {
                                relatedEntities.add(entity);
                            }
                        }

                        relationField.set(newEntity, relatedEntities);
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

    private <T extends Model> Result<T, String> checkNewEntityRelations(T newEntity, boolean force) {
        String modelKey = getModelKey(newEntity.getClass());
        @SuppressWarnings("unchecked")
        Class<T> modelClass = (Class<T>) newEntity.getClass();
        Field[] modelFields = modelClass.getFields();
        for (Field field : modelFields) {
            // https://learn.microsoft.com/en-us/dotnet/api/microsoft.entityframeworkcore.dbset-1.update?view=efcore-7.0#microsoft-entityframeworkcore-dbset-1-update(-0)
            // A recursive search of the navigation properties will be performed to find reachable entities that
            // are not already being tracked by the context. All entities found will be tracked by the context.

            InverseProperty inverseProperty;
            if ((inverseProperty = field.getAnnotation(InverseProperty.class)) != null) {
                field.setAccessible(true);
                try {
                    String fieldName = field.getName();
                    Collection<? extends Model> relatedEntities = (Collection<? extends Model>) field.get(newEntity);
                    String relatedModelName = fieldName.substring(0, fieldName.length() - 1);
                    Class<? extends Model> relatedModelClass = inverseProperty.relationModelClass();
                    Field relatedModelRelationField = relatedModelClass.getField(inverseProperty.relationModelRelationFieldName());
                    Field relatedModelRelationIdField = relatedModelClass.getField(inverseProperty.relationModelRelationIdFieldName());
                    relatedModelRelationField.setAccessible(true);
                    relatedModelRelationIdField.setAccessible(true);

                    int changes = 0;
                    for (Model relatedEntity : relatedEntities) {
                        String relationId = (String) relatedModelRelationIdField.get(relatedEntity);
                        if (!relationId.isEmpty() && !force) {
                            resetEntitiesMapsChangeFlag();
                            return Result.err(ENTITY_ALREADY_RELATED);
                        } else {
                            // We would need to roll back these changes, but the SimpleDataStore is more adapted to
                            // the behavior of this application (a single operation at a time), so it shouldn't be needed
                            relatedModelRelationField.set(relatedEntity, newEntity);
                            relatedModelRelationIdField.set(relatedEntity, newEntity.getId());
                            changes++;
                        }
                    }

                    incrementEntitiesMapChangeCounter(relatedModelName, changes);
                } catch (Exception ex) {
                    // TODO
                }
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
        PrimaryKey primaryKey;
        if ((primaryKey = primaryKeyField.getAnnotation(PrimaryKey.class)).autogenerate()) {
            primaryKeyField.setAccessible(true);

            try {
                primaryKeyField.set(newEntity, UUID.randomUUID().toString());
            } catch (IllegalAccessException e) {
                // TODO
            }
            primaryKeyField.setAccessible(false);
        } else {
            try {
                String[] composerAttributeNames = primaryKey.composerAttributeNames();
                String[] composerAttributeValues = new String[composerAttributeNames.length];

                for (int i = 0; i < composerAttributeNames.length; ++i) {

                    Field composerField = entityClass.getField(composerAttributeNames[i]);
                    composerAttributeValues[i] = composerField.get(newEntity).toString();

                }
                Method composerMethod = null;
                Method[] modelClassMethods = entityClass.getMethods();

                for (Method method : modelClassMethods) {
                    if (method.getName().equals(primaryKey.composerMethodName())) {
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

    @Override
    public Optional<Class<? extends Model>> getClassFromSimpleName(String modelClassSimpleName) {
        var foundInfo = modelInfoByKey.get(modelClassSimpleName);
        return Optional.ofNullable(foundInfo != null ? foundInfo.modelClass : null);
    }

    @Override
    public <T extends Model> Result<T, String> add(T newEntity) {
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(entityClass);
        Result<Map<String, T>, String> result = getAll(modelKey);
        assignPrimaryKey(newEntity);

        return result.andThen(m -> {
            if (!m.containsKey(newEntity.getId())) {
                m.put(newEntity.getId(), newEntity);
                return checkNewEntityRelations(newEntity, false).map(e -> {
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
                T entity = m.remove(id);
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
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(entityClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.andThen(m -> checkNewEntityRelations(newEntity, force).map(e -> {
            String entityId = newEntity.getId();
            T existingEntity = m.get(entityId);
            if (existingEntity != null) {
                // newEntity and existingEntity could be referencing the same instance, but the replace operation is
                // cheap, so a check for equality is not so necessary
                m.replace(entityId, newEntity);
            } else {
                assignPrimaryKey(newEntity);
                m.put(entityId, newEntity);
            }

            incrementEntitiesMapChangeCounter(modelKey);

            return e;
        }));
    }

    @Override
    public Result<Integer, Exception> commitChanges() {
        Gson gson = createGson();

        for (Map.Entry<String, Optional<Map<String, ? extends Model>>> entry : entitiesByName.entrySet()) {
            if (changesMadeTo(entry.getKey())) {
                Map<String, ? extends Model> map = entry.getValue().get();
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
        public int changeCounter;

        public ModelInfo(Class<T> modelClass, Field primaryKeyField, List<Field> foreignKeyFields) {
            this.modelClass = modelClass;
            this.primaryKeyField = primaryKeyField;
            this.foreignKeyFields = foreignKeyFields;
        }
    }
}
