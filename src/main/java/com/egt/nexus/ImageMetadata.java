package com.egt.nexus;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "items")
public class ImageMetadata {
    private List<MetadataItem> items;
}

@Data
class MetadataItem {
    private String id;
    private String repository;
    private String format;
    private String group;
    private String name;
    private String version;
}