package com.una.programming_two_first_project.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.stream.JsonReader;
import com.una.programming_two_first_project.model.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SimpleDataStore implements DataStore
{
    private final Executor singleThreadExecutor;
    private final Map<String, Collaborator> collaborators;
    private final Map<String, Department> departments;
    private final Map<String, Project> projects;
    private final Map<String, Sprint> sprints;
    private final Map<String, Task> tasks;
    private final Map<String, Class<? extends Model>> modelTypesByKey;
    private final Map<String, Map<String,? extends Model>> modelsByName;
    private final String applicationDataFolderPath;

    public SimpleDataStore(String applicationDataFolderPath) {
        this.applicationDataFolderPath = applicationDataFolderPath;

        collaborators = new HashMap<>();
        departments = new HashMap<>();
        projects = new HashMap<>();
        sprints = new HashMap<>();
        tasks = new HashMap<>();
        modelsByName = Map.of(
                Collaborator.class.getSimpleName().toLowerCase(), collaborators,
                Department.class.getSimpleName().toLowerCase(), departments,
                Project.class.getSimpleName().toLowerCase(), projects,
                Sprint.class.getSimpleName().toLowerCase(), sprints,
                Task.class.getSimpleName().toLowerCase(), tasks);
        modelTypesByKey = Map.of(
                Collaborator.class.getSimpleName().toLowerCase(), Collaborator.class,
                Department.class.getSimpleName().toLowerCase(), Department.class,
                Project.class.getSimpleName().toLowerCase(), Project.class,
                Sprint.class.getSimpleName().toLowerCase(), Sprint.class,
                Task.class.getSimpleName().toLowerCase(), Task.class);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
    }

    private <T extends Model> void loadEntities(String modelKey) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Department.class, new ModelTypeAdapter(this))
                .create();

        Map<String, T> entityMap = (Map<String, T>) modelsByName.get(modelKey);
        Path filePath = Path.of(applicationDataFolderPath, "programming_two_second_project", modelKey + ".json");
        File file = filePath.toFile();
        try (FileReader fileReader = new FileReader(file); BufferedReader bufferedReader = new BufferedReader(fileReader);
             JsonReader jsonReader = gson.newJsonReader(bufferedReader)) {
            Class<? extends Model> modelClass = modelTypesByKey.get(modelKey);
            Constructor<? extends Model> modelConstructor = modelClass.getConstructor();
            Map<String, Field> modelFields = Arrays.stream(modelClass.getFields())
                    .peek(p -> p.setAccessible(true))
                    .collect(Collectors.toMap(Field::getName, Function.identity()));

            jsonReader.beginArray();
            while (jsonReader.hasNext()) {
                T newEntity = (T) modelConstructor.newInstance();
                jsonReader.beginObject();

                while (jsonReader.hasNext()) {
                    String attributeName = jsonReader.nextName();
                    String typeName = jsonReader.peek().toString();

                    var field = modelFields.get(attributeName);

                    switch (typeName) {
                        case "BOOLEAN":
                            jsonReader.nextBoolean();
                            break;

                        case "STRING":
                            Object value = jsonReader.nextString();

                            if (Model.class.isAssignableFrom(field.getType())) {
                                String id = (String) value;
                                if (!id.isEmpty()) {
                                    var result = get(field.getType().getSimpleName().toLowerCase(), id);

                                    if (result.isOk()) {
                                        value = result.get();
                                    }
                                }
                            } else if (value == null) {
                                value = "";
                            }

                            field.set(newEntity, value);
                            break;
                    }
                }
                jsonReader.endObject();
                entityMap.put(newEntity.getId(), newEntity);
            }
        } catch (Exception ex) {
            System.err.println(ex);
        }

//        try {
//
//            CompletableFuture.runAsync(() -> {
//
//
//            }, singleThreadExecutor).get();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } catch (ExecutionException e) {
//            throw new RuntimeException(e);
//        }
    }

    public <T extends Model> Result<T, String> get(String modelKey, String id) {
        Result<Map<String, T>, String> result = getAll(modelKey);
        return result.andThen(m -> {
            T model = m.get(id);

            if (model != null) {
                return Result.ok(model);
            }

            return Result.err(DataStore.ENTITY_NOT_FOUND);
        });
    }

//    public <T extends Model> Result<T, String> get(Class<T> modelClass, String id) {
//        Result<Map<String, T>, String> result = getAll(modelClass);
//        return result.andThen(m -> {
//            T model = m.get(id);
//
//            if (model != null) {
//                return Result.ok(model);
//            }
//
//            return Result.err(DataStore.ENTITY_NOT_FOUND);
//        });
//    }

    public <T extends Model> Result<Map<String, T>, String> getAll(String modelKey) {
        Map<String, T> map = (Map<String, T>) modelsByName.get(modelKey);

        if (map == null) {
            return Result.err(DataStore.MODEL_NOT_FOUND);
        }

        if (map.isEmpty()) {
            loadEntities(modelKey);
        }

        return Result.ok(map);
    }
}
