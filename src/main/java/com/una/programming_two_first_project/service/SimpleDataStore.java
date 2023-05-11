package com.una.programming_two_first_project.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.inject.Inject;
import com.una.programming_two_first_project.model.*;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
    private final Map<String, Tuple<Class<? extends Model>, Integer>> modelInfoByKey;
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
        entitiesByName = new HashMap<>(Map.of(
                getModelKey(Collaborator.class),    Optional.empty(),
                getModelKey(Department.class),      Optional.empty(),
                getModelKey(Project.class),         Optional.empty(),
                getModelKey(Sprint.class),          Optional.empty(),
                getModelKey(Task.class),            Optional.empty()));
        modelInfoByKey = new HashMap<>(Map.of(
                getModelKey(Collaborator.class),    tuple(Collaborator.class, 0),
                getModelKey(Department.class),      tuple(Department.class, 0),
                getModelKey(Project.class),         tuple(Project.class, 0),
                getModelKey(Sprint.class),          tuple(Sprint.class, 0),
                getModelKey(Task.class),            tuple(Task.class, 0)));
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

//    private boolean areEntitiesLoaded(String modelKey) {
//        return entitiesByName.unwrap(modelKey).isPresent();
//    }

    private boolean changesMadeTo(String modelKey) {
        return modelInfoByKey.get(modelKey).y() > 0;
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
            Class<? extends Model> modelClass = modelInfoByKey.get(modelKey).x();
            Constructor<? extends Model> modelConstructor = modelClass.getConstructor();
            List<Tuple<Field, Field>> relationFields = new ArrayList<>();
            Map<String, Field> modelFields = Arrays.stream(modelClass.getFields())
                    .peek(f -> {
                        f.setAccessible(true);

                        try {
                            if (Model.class.isAssignableFrom(f.getType())) {
                                String relationFieldName = f.getName();
                                String relationIdFieldName = String.format("%sId", relationFieldName);
                                Field relationIdField = modelClass.getField(relationIdFieldName);

                                relationFields.add(tuple(f, relationIdField));
                            } else if (Collection.class.isAssignableFrom(f.getType())) {
                                String relatedModelKey = f.getName();
                                relatedModelKey = relatedModelKey.substring(0, relatedModelKey.length() - 1);
                                Class<? extends Model> relatedModelType = modelInfoByKey.get(relatedModelKey).x();
                                Field relatedModelRelationIdField = relatedModelType.getField(String.format("%sId", modelKey));
                                relatedModelRelationIdField.setAccessible(true);

                                relationFields.add(tuple(f, relatedModelRelationIdField));
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
                    Class<?> relationFieldType = relationField.getType();
                    if (Model.class.isAssignableFrom(relationFieldType)) {
                        String relatedEntityId = (String) relationIdField.get(newEntity);
                        if (!relatedEntityId.isEmpty()) {
                            Result<Optional<Model>, String> result = get(getModelKey((Class<? extends Model>) relationField.getType()), relatedEntityId);

                            result.inspect(o -> {
                                // TODO: Why use Optional then...
                                Model value = o.orElse(null);
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
            System.err.println(ex);
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

    private void incrementEntitiesMapChangeCounter(String modelKey) {
        Tuple<Class<? extends Model>, Integer> tuple = modelInfoByKey.get(modelKey);
        modelInfoByKey.replace(modelKey, tuple(tuple.x(), tuple.y() + 1));
    }

    private int resetEntitiesMapsChangeFlag() {
        int allChangesCount = 0;
        for (Map.Entry<String, Tuple<Class<? extends Model>, Integer>> entry : modelInfoByKey.entrySet()) {
            Tuple<Class<? extends Model>, Integer> tuple = entry.getValue();
            modelInfoByKey.replace(entry.getKey(), tuple(tuple.x(), 0));
            allChangesCount += tuple.y();
        }

        return allChangesCount;
    }

    @Override
    public <T extends Model> Result<T, String> add(T newEntity) {
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(entityClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.andThen(m -> {
            if (!m.containsKey(newEntity.getId())) {
                m.put(newEntity.getId(), newEntity);
                incrementEntitiesMapChangeCounter(modelKey);
                return Result.ok(newEntity);
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
    public <T extends Model> Result<T, String> update(T newEntity) {
        Class<T> entityClass = (Class<T>) newEntity.getClass();
        String modelKey = getModelKey(entityClass);
        Result<Map<String, T>, String> result = getAll(modelKey);

        return result.map(m -> {
            String entityId = newEntity.getId();
            T existingEntity = m.get(entityId);

            if (existingEntity != null) {
                m.replace(entityId, newEntity);
            } else {
                m.put(entityId, newEntity);
            }

            incrementEntitiesMapChangeCounter(modelKey);

//            @SuppressWarnings("unchecked")
//            Class<T> modelClass = (Class<T>) newEntity.getClass();
//            Field[] modelFields = modelClass.getFields();
//            for (Field field : modelFields) {
//                // TODO: Relation field annotation like EF Core?
//                // https://learn.microsoft.com/en-us/dotnet/api/microsoft.entityframeworkcore.dbset-1.update?view=efcore-7.0#microsoft-entityframeworkcore-dbset-1-update(-0)
//                // A recursive search of the navigation properties will be performed to find reachable entities that
//                // are not already being tracked by the context. All entities found will be tracked by the context.
//
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
//            }

            return newEntity;
        });
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
                    gson.toJson(entities, modelInfoByKey.get(modelKey).x().arrayType(), jsonWriter);
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
}
