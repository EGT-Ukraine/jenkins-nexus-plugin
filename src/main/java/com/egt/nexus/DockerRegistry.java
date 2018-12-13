package com.egt.nexus;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Data
@XmlRootElement(name = "repositories")
public class DockerRegistry {
    private List<String> repositories;
}
