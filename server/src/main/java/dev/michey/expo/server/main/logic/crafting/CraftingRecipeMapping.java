package dev.michey.expo.server.main.logic.crafting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;

public class CraftingRecipeMapping {

    private static CraftingRecipeMapping INSTANCE;

    private final HashMap<Integer, List<String>> categoryMap;
    private final HashMap<String, CraftingRecipe> recipeMap;

    public CraftingRecipeMapping(boolean client) {
        recipeMap = new HashMap<>();

        FileHandle fh = Gdx.files.internal("crafting.json");
        JSONObject json = new JSONObject(fh.readString());

        JSONArray recipes = json.getJSONArray("recipes");

        for(int i = 0; i < recipes.length(); i++) {
            JSONObject recipe = recipes.getJSONObject(i);
            CraftingRecipe cr = new CraftingRecipe();

            String identifier = recipe.getString("identifier");
            int outputId = recipe.getJSONObject("output").getInt("id");
            int outputAmount = recipe.getJSONObject("output").getInt("amount");

            cr.recipeIdentifier = identifier;
            cr.outputId = outputId;
            cr.outputAmount = outputAmount;

            JSONArray inputArray = recipe.getJSONArray("input");

            cr.inputIds = new int[inputArray.length()];
            cr.inputAmounts = new int[inputArray.length()];

            for(int j = 0; j < inputArray.length(); j++) {
                JSONObject input = inputArray.getJSONObject(j);

                cr.inputIds[j] = input.getInt("id");
                cr.inputAmounts[j] = input.getInt("amount");
            }

            recipeMap.put(identifier, cr);
        }

        log("Added " + recipeMap.size() + " crafting recipe entries.");

        if(client) {
            categoryMap = new HashMap<>();

            JSONObject categories = json.getJSONObject("categories");

            {
                categoryMap.put(ExpoShared.CRAFTING_CATEGORY_MISC, new LinkedList<>());
                JSONArray misc = categories.getJSONArray("misc");
                for(int i = 0; i < misc.length(); i++) {
                    String s = misc.getString(i);
                    categoryMap.get(ExpoShared.CRAFTING_CATEGORY_MISC).add(s);
                }
            }

            {
                categoryMap.put(ExpoShared.CRAFTING_CATEGORY_TOOLS, new LinkedList<>());
                JSONArray tools = categories.getJSONArray("tools");
                for(int i = 0; i < tools.length(); i++) {
                    String s = tools.getString(i);
                    categoryMap.get(ExpoShared.CRAFTING_CATEGORY_TOOLS).add(s);
                }
            }

            {
                categoryMap.put(ExpoShared.CRAFTING_CATEGORY_FOOD, new LinkedList<>());
                JSONArray food = categories.getJSONArray("food");
                for(int i = 0; i < food.length(); i++) {
                    String s = food.getString(i);
                    categoryMap.get(ExpoShared.CRAFTING_CATEGORY_FOOD).add(s);
                }
            }
        } else {
            categoryMap = null;
        }

        INSTANCE = this;
    }

    public HashMap<Integer, List<String>> getCategoryMap() {
        return categoryMap;
    }

    public HashMap<String, CraftingRecipe> getRecipeMap() {
        return recipeMap;
    }

    public static CraftingRecipeMapping get() {
        return INSTANCE;
    }

}
