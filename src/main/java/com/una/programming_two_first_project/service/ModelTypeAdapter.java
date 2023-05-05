package com.una.programming_two_first_project.service;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.una.programming_two_first_project.model.Model;
import com.una.programming_two_first_project.model.Result;

import java.io.IOException;

public class ModelTypeAdapter extends TypeAdapter
{
    private final DataStore dataStore;

    public ModelTypeAdapter(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public void write(JsonWriter jsonWriter, Object o) throws IOException {

    }

    @Override
    public Object read(JsonReader jsonReader) throws IOException {
//        String path = jsonReader.getPath();
//        int lastDotIndex = path.lastIndexOf('.');
//        String attributeName = path.substring(lastDotIndex + 1);
//
//        Result<Model, String> result = dataStore.get(attributeName, jsonReader.nextString());
//        return result.okOrElse(null);
        return null;
    }
}
