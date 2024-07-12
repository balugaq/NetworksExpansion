package com.ytdd9527.networks.expansion.core.item.machine.stackmachine.attribute;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class WorkingRecipe {
    public String recipeName;
    public int duration;
    public int energy;
    public HashMap<ItemStack, Integer> inputs;
    public HashMap<ItemStack, Integer> outputs;
    public HashMap<ItemStack, Integer> weights;

    public WorkingRecipe(String recipeName, int duration, int energy, HashMap<ItemStack, Integer> inputs, HashMap<ItemStack, Integer> outputs, HashMap<ItemStack, Integer> weights) {
        this.recipeName = recipeName;
        this.duration = duration;
        this.energy = energy;
        this.inputs = inputs;
        this.outputs = outputs;
        this.weights = weights;
    }

    public boolean isMatch(ItemStack[] incomings) {
        // 创建一个映射来存储传入物品栈的数量
        HashMap<ItemStack, Integer> incomingCounts = new HashMap<ItemStack, Integer>(4, 0.75f);
        for (ItemStack item : incomings) {
            incomingCounts.merge(item, item.getAmount(), Integer::sum);
        }

        // 检查输入是否匹配
        for (Map.Entry<ItemStack, Integer> entry : this.inputs.entrySet()) {
            ItemStack input = entry.getKey();
            Integer requiredAmount = entry.getValue();
            Integer incomingAmount = incomingCounts.getOrDefault(input, 0);
            if (incomingAmount < requiredAmount) {
                return false;
            }
        }

        return true;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getEnergy() {
        return this.energy;
    }

    public HashMap<ItemStack, Integer> getInputs() {
        return this.inputs;
    }

    public HashMap<ItemStack, Integer> getOutputs() {
        return this.outputs;
    }

    public HashMap<ItemStack, Integer> getWeights() {
        return this.weights;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
    }

    public void setInputs(HashMap<ItemStack, Integer> inputs) {
        this.inputs = inputs;
    }

    public void setOutputs(HashMap<ItemStack, Integer> outputs) {
        this.outputs = outputs;
    }

    public void setWeights(HashMap<ItemStack, Integer> weights) {
        this.weights = weights;
    }

    public HashMap<ItemStack, Integer> outputItems() {
        HashMap<ItemStack, Integer> goings = new HashMap<ItemStack, Integer>(4, 0.75f);
        Random random = new Random();
        for (ItemStack outputItem: this.outputs.keySet()) {
            int weight = this.weights.get(outputItem);
            if (weight == 100) {
                goings.put(outputItem, this.outputs.get(outputItem));
            } else {
                if (random.nextInt(100) < weight) {
                    goings.put(outputItem, this.outputs.get(outputItem));
                }
            }
        }
        return goings;
    }
}
