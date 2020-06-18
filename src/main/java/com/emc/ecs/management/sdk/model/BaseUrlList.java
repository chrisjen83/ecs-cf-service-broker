package com.emc.ecs.management.sdk.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "base_urls")
public class BaseUrlList {

    private List<BaseUrl> baseUrls;

    public BaseUrlList() {
        super();
    }

    public BaseUrlList(List<BaseUrl> baseUrls) {
        super();
        this.baseUrls = baseUrls;
    }

    @XmlElement(name = "base_url")
    public List<BaseUrl> getBaseUrls() {
        return baseUrls;
    }

    public void setBaseUrls(List<BaseUrl> baseUrls) {
        this.baseUrls = baseUrls;
    }

}
