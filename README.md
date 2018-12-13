jenkins-nexus-plugin
---

You could add whole space to deployment from Nexus' Docker Registry using image tags 

### Run dev env:
```
make dev
```

### Build
```
make build
```

### Result

Containers, inside `go-team`-space are placed by following repo urls:

    - nexus.egt.com/go-team/protoc
    - nexus.egt.com/go-team/imageproxy
    - nexus.egt.com/go-team/imageproxy-img-proxy

Create project:
![create](https://raw.githubusercontent.com/EGT-Ukraine/jenkins-nexus-plugin/master/docs/images/1.png)

Check and Setup `Nexus Integration`:
![setup](https://raw.githubusercontent.com/EGT-Ukraine/jenkins-nexus-plugin/master/docs/images/2.png)

Build:
![build](https://raw.githubusercontent.com/EGT-Ukraine/jenkins-nexus-plugin/master/docs/images/3.png)

It will create local variable with your choices which you could use in your scripts:
```
PROJECT_DATA:[protoc:latest, imageproxy:0.1.4, imageproxy-img-proxy:master]
```