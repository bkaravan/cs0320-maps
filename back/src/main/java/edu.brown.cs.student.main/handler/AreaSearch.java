package edu.brown.cs.student.main.handler;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import edu.brown.cs.student.main.handler.BoundBox.FeatureCollection;
import edu.brown.cs.student.main.handler.BoundBox.Features;
import edu.brown.cs.student.main.handler.BoundBox.Geometry;
import java.io.File;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import spark.Request;
import spark.Response;
import spark.Route;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AreaSearch implements Route {

  private Map<String, FeatureCollection> searchHistory;

  public AreaSearch() {
//    this.collection = collection;
  }

  @Override
  public Object handle(Request request, Response response) {

    String file = request.queryParams("filepath");
    String keyword = request.queryParams("keyword");

    if (file == null || file.isEmpty() || !isFileValid(file)) {
      return new LoadHandler.FailureResponse("Error: Invalid or empty file name").serialize();
    }

    if (keyword == null) {
      return new LoadHandler.FailureResponse("Error: missing keyword parameter").serialize();
    }

    Moshi moshi = new Moshi.Builder().build();
    JsonAdapter<FeatureCollection> jsonAdapter = moshi.adapter(FeatureCollection.class);

    if (this.searchHistory.containsKey(keyword)){
      return jsonAdapter.toJson(this.searchHistory.get(keyword));
    }
    try {
      Path filePath = Paths.get(file);
      String json = Files.readString(filePath);

      FeatureCollection data = jsonAdapter.fromJson(json);

      AreaSearch.Features[] fillArray = new AreaSearch.Features[data.features.length];
      AreaSearch.FeatureCollection returnData = new AreaSearch.FeatureCollection(data.type, fillArray);

      int i = 0;
      for (Features feat : data.features) {
        //System.out.println(feat);
        if (feat.properties == null) {
          continue;
        }
        Properties currProps = feat.properties;
        Map<String, String> descriptions = currProps.area_description_data;
        for (String description : descriptions.values()) {
          if (description.contains(keyword)) {
            Properties property = feat.properties.changeHolcGrade("H");
            feat = new Features(feat.geometry, property, feat.type);
          }
        }
          fillArray[i] = feat;
          i++;
      }
//      Type mapStringObject = Types.newParameterizedType(Map.class, String.class, Object.class);
//      JsonAdapter<Map<String, Object>> adapter = moshi.adapter(mapStringObject);
////    JsonAdapter<CensusData> censusDataAdapter = moshi.adapter(CensusData.class);
//      Map<String, Object> responseMap = new LinkedHashMap<>();
//      responseMap.put("Message", "Keyword: \"" + keyword + "\" search has been conducted!");
      this.searchHistory.put(keyword, returnData);
      return jsonAdapter.toJson(returnData);
      //return adapter.toJson(responseMap);
  // do something with return data
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }

  }


    private boolean isFileValid(String fileName) {
      // Check if the file exists and is a valid file (not a directory)
      File file = new File(fileName);
      return file.exists() && file.isFile();
    }

  private boolean containsKeyword(String description, String keyword) {
    //return (this.minLat < x) && (this.maxLat > x) && (this.minLon < y) && (this.maxLon > y);
    return true;
  }

  public record FeatureCollection(String type, Features[] features){}
  public record Features(Geometry geometry, Properties properties, String type){}
  public record Geometry(String type, List<List<List<List<Float>>>> coordinates){}
  public record Properties(Map<String, String> area_description_data, String city,
                           String holc_grade, String name){
    public Properties changeHolcGrade(String newHolcGrade) {
      return new Properties(area_description_data, city, newHolcGrade, name);
    }
  }

}

