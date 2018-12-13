package com.egt;

import hudson.model.ChoiceParameterDefinition;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProjectChoiceParameters {

    private char emptyItemSymbol;
    private String itemSeparator;

    public ProjectChoiceParameters(char emptyItemSymbol, String separator) {
        this.emptyItemSymbol = emptyItemSymbol;
        this.itemSeparator = separator;
    }

    public ParametersDefinitionProperty createParams(Map<String, String> params) {
        List<ParameterDefinition> paramDefs = new ArrayList<>(params.size());

        for (Map.Entry<String, String> param : params.entrySet()) {
            String[] tags = String.format("%s;%s", emptyItemSymbol, param.getValue()).split(itemSeparator);
            paramDefs.add(new ChoiceParameterDefinition(param.getKey(), tags, ""));
        }


        return new ParametersDefinitionProperty(paramDefs);
    }
}
