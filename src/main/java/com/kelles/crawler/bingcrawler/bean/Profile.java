package com.kelles.crawler.bingcrawler.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kelles.crawler.bingcrawler.setting.Constant;
import org.apache.http.util.TextUtils;

/*结构:
	Academic->title 文章标题
	        ->panels 作者,摘要,会议,关键词


	        Panel->label 如Authors,Introduction
	             ->snippets Map<"snippet","url"> 如<Kalyanmoy Deb,指向该作者的主页链接>*/
public class Profile implements Serializable {
    private java.lang.String title; //文章标题
    private java.lang.String url; //指向这篇文章的必应学术Profile页面
    private List<b_hPanel> panels = new ArrayList(); //作者,摘要,会议
    private List<Profile> references = new ArrayList(); //引用的文章(其中Profile只包含标题,作者和链接)
    private List<Profile> citedPapers = new ArrayList(); //引用这篇的文章(其中Profile只包含标题,作者和链接)
    private List<java.lang.String> downloadUrls = new ArrayList(); //下载链接
    private List<java.lang.String> sourceUrls = new ArrayList(); //来源链接

    //获取作者名
    public List<java.lang.String> getAuthors() {
        return getPanelItems(Constant.Authors, SnippetType.snippet);
    }

    //获取作者链接
    public List<java.lang.String> getAuthorsUrl() {
        return getPanelItems(Constant.Authors, SnippetType.url);
    }

    //获取摘要(有多个snippet时合并在一起)
    public java.lang.String getIntroduction() {
        List<java.lang.String> items = getPanelItems(Constant.Introduction, SnippetType.snippet);
        if (items != null && items.size() > 0) {
            java.lang.String result = "";
            for (java.lang.String item : items) result += item + " ";
            return result;
        }
        return null;
    }

    //获取关键字
    public List<java.lang.String> getKeywords() {
        return getPanelItems(Constant.Keywords, SnippetType.snippet);
    }

    //获取关键字链接
    public List<java.lang.String> getKeywordsUrl() {
        return getPanelItems(Constant.Keywords, SnippetType.url);
    }

    //获取年份
    public java.lang.String getYear() {
        List<java.lang.String> items = getPanelItems(Constant.Year, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取会议
    public java.lang.String getJournal() {
        List<java.lang.String> items = getPanelItems(Constant.Journal, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取会议链接
    public java.lang.String getJournalUrl() {
        List<java.lang.String> items = getPanelItems(Constant.Journal, SnippetType.url);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取卷号
    public java.lang.String getVolumn() {
        List<java.lang.String> items = getPanelItems(Constant.Volume, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取期号
    public java.lang.String getIssue() {
        List<java.lang.String> items = getPanelItems(Constant.Issue, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取页码范围
    public java.lang.String getPages() {
        List<java.lang.String> items = getPanelItems(Constant.Pages, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取被引量
    public java.lang.String getCitedBy() {
        List<java.lang.String> items = getPanelItems(Constant.Cited_By, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //获取DOI
    public java.lang.String getDOI() {
        List<java.lang.String> items = getPanelItems(Constant.DOI, SnippetType.snippet);
        if (items != null && items.size() > 0) return items.get(0);
        return null;
    }

    //工具方法

    //获取某个标签下的元素,如Authors,Keywords,Journal,不存在返回null
    private enum SnippetType {
        snippet, url
    }

    private List<java.lang.String> getPanelItems(java.lang.String label, SnippetType type) {
        java.lang.String typeStr = null;
        if (type == SnippetType.snippet) typeStr = "snippet";
        else if (type == SnippetType.url) typeStr = "url";
        for (b_hPanel panel : panels)
            if (label.equals(panel.getLabel())) {
                List<java.lang.String> urls = new ArrayList();
                for (Map<java.lang.String, java.lang.String> map : panel.getSnippets())
                    if (map.containsKey(typeStr)) urls.add(map.get(typeStr));
                return urls;
            }
        return null;
    }

    //为List<Map<String,String>>添加一条记录
    private void addSnippet(java.lang.String snippet, java.lang.String url, List<Map<java.lang.String, java.lang.String>> snippets) {
        if (TextUtils.isEmpty(snippet)) return;
        Map<java.lang.String, java.lang.String> map = new HashMap();
        map.put("snippet", snippet);
        if (!TextUtils.isEmpty(url)) map.put("url", url);
        snippets.add(map);
    }


    //Getters & Setters
    public java.lang.String getTitle() {
        return title;
    }

    public List<Profile> getCitedPapers() {
        return citedPapers;
    }


    public void setCitedPapers(List<Profile> citedPapers) {
        this.citedPapers = citedPapers;
    }


    public List<java.lang.String> getDownloadUrls() {
        return downloadUrls;
    }


    public void setDownloadUrls(List<java.lang.String> downloadUrls) {
        this.downloadUrls = downloadUrls;
    }


    public List<java.lang.String> getSourceUrls() {
        return sourceUrls;
    }


    public void setSourceUrls(List<java.lang.String> sourceUrls) {
        this.sourceUrls = sourceUrls;
    }


    public java.lang.String getUrl() {
        return url;
    }


    public void setUrl(java.lang.String url) {
        this.url = url;
    }


    public List<Profile> getReferences() {
        return references;
    }


    public void setReferences(List<Profile> references) {
        this.references = references;
    }


    public void setTitle(java.lang.String title) {
        this.title = title;
    }

    public List<b_hPanel> getPanels() {
        return panels;
    }

    public void setPanels(List<b_hPanel> panels) {
        this.panels = panels;
    }


    @Override
    public java.lang.String toString() {
        java.lang.String str = "Profile [title=" + title + ", url=" + url + ", panels=" + panels;
        if (references.size() > 0) str += ", \nreferences=" + references;
        if (citedPapers.size() > 0) str += ", \ncitedPapers=" + citedPapers;
        if (downloadUrls.size() > 0) str += ", \ndownloadUrls=" + downloadUrls;
        if (sourceUrls.size() > 0) str += ", \nsourceUrls=" + sourceUrls;
        str += "]";
        return str;
    }


}
