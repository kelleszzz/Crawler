package com.kelles.crawler.crawler.database;


import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kelles.crawler.crawler.util.*;
import com.kelles.crawler.crawler.bean.*;
import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.SecondaryCursor;
import com.sleepycat.je.Transaction;


public class UrlsDbManager {
    private UrlsDb db = null;
    private int maxDepth = Integer.MAX_VALUE;
    private String homeDirPath = null;

    public UrlsDbManager(String homeDirPath) {
        super();
        this.homeDirPath = homeDirPath;
        setup();
    }

    public static void main(String[] args) throws Exception {
        UrlsDbManager manager = null;
        try {
            manager = new UrlsDbManager("DbManagerTest");
            CrawlUrl u1 = new CrawlUrl("http://www.hacg.fi/wp/23147.html#comment-62635");
            CrawlUrl u2 = new CrawlUrl("http://www.bing.com");
            CrawlUrl u3 = new CrawlUrl("http://www.w3school.com.cn/");
            CrawlUrl u4 = new CrawlUrl("http://blog.csdn.net/shangboerds/article/details/7532676");
            CrawlUrl u5 = new CrawlUrl("https://www.hacg.li");
            u3.setWeight(150);
            u4.setWeight(80);
            manager.putUrl(u1);
            manager.putUrl(u2);
            manager.putUrl(u3);
            manager.putUrl(u4);
            manager.putUrl(u5);

            Logger.log("获取weight最高的条目:\n" + manager.getNextCrawlUrl());

            SecondaryCursor secCursor = manager.db.todoUrlsByWeight.openSecondaryCursor(null, null);
            DatabaseEntry searchKey = new DatabaseEntry(Util.intToByteArray(100));
            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundValue = new DatabaseEntry();
            Logger.log("遍历weightSecDb的条目");
            OperationStatus retVal = secCursor.getFirst(foundKey, foundValue, LockMode.DEFAULT);
            for (; ; ) {
                if (retVal == OperationStatus.SUCCESS) {
                    CrawlUrl crawlUrl = (CrawlUrl) manager.db.serialBinding.entryToObject(foundValue);
                    Logger.log("weight = " + crawlUrl.getWeight() + ":\n" + crawlUrl);
//					secCursor.delete();
                } else break;
                retVal = secCursor.getNext(foundKey, foundValue, LockMode.DEFAULT);
            }
            Logger.log("搜索weight=100的条目");
            retVal = secCursor.getSearchKey(searchKey, foundKey, foundValue, LockMode.DEFAULT);
            for (; ; ) {
                if (retVal == OperationStatus.SUCCESS) {
                    CrawlUrl crawlUrl = (CrawlUrl) manager.db.serialBinding.entryToObject(foundValue);
                    Logger.log("weight = " + crawlUrl.getWeight() + ":\n" + crawlUrl);
//					secCursor.delete();
                } else break;
                retVal = secCursor.getNextDup(foundKey, foundValue, LockMode.DEFAULT);
            }
            manager.db.describe(true, true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            manager.close();
        }
    }

    private void setup() {
        if (db == null) {
            db = new UrlsDb(homeDirPath);
            Logger.log(11.14, "加载UrlsDbManager[" + homeDirPath + "]");
        }
    }

    public void close() {
        if (db != null) {
            db.close();
            db = null;
        }
    }

    /*uniUrls条目数*/
    public long sizeUniUrls() {
        Cursor cursor;
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        OperationStatus retVal = null;
        cursor = db.uniUrls.openCursor(null, null);
        if (cursor.getFirst(key, value, LockMode.DEFAULT) != OperationStatus.SUCCESS) return 0;
        long size = cursor.skipNext(Long.MAX_VALUE, key, value, LockMode.DEFAULT);
        if (cursor != null) cursor.close();
        return (size + 1);
    }

    /**
     * 获取一条消息
     *
     * @param url
     * @param key
     * @return
     */
    public String getMessage(String url, String key) {
        if (key == null) return null;
        Map<String, String> messages = getMessages(url);
        if (messages != null) {
            return messages.get(key);
        }
        return null;
    }

    /**
     * 依次尝试从todoUrls,uniUrls中获取messages
     *
     * @param url
     * @return
     */
    public Map<String, String> getMessages(String url) {
        CrawlUrl crawlUrl = getCrawlUrl(url);
        if (crawlUrl != null) {
            if (crawlUrl.getMessages() != null) {
                return crawlUrl.getMessages();
            } else {
                return new HashMap<>();
            }
        }
        return null;
    }

    //从uniUrls中获取messages
    @Deprecated
    public List<String> getMessagesFromUniUrls(String url) {
        CrawlUrl crawlUrl = getCrawlUrlFromUniUrls(url);
        if (crawlUrl != null && crawlUrl.getMessages() != null) {
            Map<String, String> messages = crawlUrl.getMessages();
            return messages != null ? new ArrayList<>(messages.values()) : null;
        }
        return null;
    }

    //从todoUrls中获取已存在的SimHash
    public BigInteger getSimHash(String url) {
        CrawlUrl crawlUrl = getCrawlUrlFromTodoUrls(url);
        if (crawlUrl != null && crawlUrl.getSimHash() != null) return crawlUrl.getSimHash();
        return null;
    }

    //获取所有uniUrls中的SimHash值
    public List<Map<String, Object>> getAllSimHashs() {
        Transaction txn = null;
        Cursor cursor = null;
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        List<Map<String, Object>> simHashs = new ArrayList<Map<String, Object>>();

        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            cursor = db.uniUrls.openCursor(txn, null);
            OperationStatus retVal = cursor.getFirst(key, value, LockMode.DEFAULT);
            for (; ; ) {
                if (retVal == OperationStatus.SUCCESS) {
                    CrawlUrl crawlUrl = (CrawlUrl) db.serialBinding.entryToObject(value);
                    if (crawlUrl.getSimHash() != null) {
                        Map<String, Object> map = new HashMap();
                        map.put("url", crawlUrl.getUrl());
                        map.put("simHash", crawlUrl.getSimHash());
                        simHashs.add(map);
                    }
                } else break;
                retVal = cursor.getNext(key, value, LockMode.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) cursor.close();
            if (txn != null) txn.commit();
        }

        return simHashs;
    }

    /**
     * 存入一条消息
     *
     * @param url
     * @param key
     * @param message
     * @return
     */
    public OperationStatus putMessage(String url, String key, String message) {
        if (key == null) return OperationStatus.KEYEMPTY;
        Map<String, String> messages = getMessages(url);
        if (message != null) {
            messages.put(key, message);
        }
        return updateMessages(url, messages);
    }

    /**
     * 依次尝试从todoUrls和uniUrls中更新messages
     *
     * @param url
     * @param messages
     * @return
     */
    protected OperationStatus updateMessages(String url, Map<String, String> messages) {
        //获取CrawlUrl
        CrawlUrl crawlUrl = getCrawlUrl(url);
        if (crawlUrl == null) return OperationStatus.NOTFOUND;
        //更新域
        if (messages == null && crawlUrl.getMessages() != null) {
            crawlUrl.getMessages().clear();
        } else {
            crawlUrl.setMessages(messages);
        }
        //依次尝试从todoUrls和uniUrls中更新messages
        OperationStatus status = updateCrawlUrlFromTodoUrls(crawlUrl);
        if (!OperationStatus.SUCCESS.equals(status)) {
            status = updateCrawlUrlFromUniUrls(crawlUrl);
        }
        return status;
    }

    /**
     * 依次尝试从todoUrls和uniUrls中获取CrawlUrl
     *
     * @param url
     * @return
     */
    protected CrawlUrl getCrawlUrl(String url) {
        CrawlUrl crawlUrl = getCrawlUrlFromTodoUrls(url);
        if (crawlUrl == null) {
            crawlUrl = getCrawlUrlFromUniUrls(url);
        }
        return crawlUrl;
    }

    //更新相应todoUrls中的messages值
    @Deprecated
    public OperationStatus updateMessagesOfTodoUrls(String url, List<String> messages) {
        CrawlUrl crawlUrl = getCrawlUrlFromTodoUrls(url);
        if (crawlUrl != null) {
            if (messages == null && crawlUrl.getMessages() != null) {
                crawlUrl.getMessages().clear();
            } else {
                Map<String, String> messageMap = new HashMap<>();
                for (String message : messages) {
                    messageMap.put(message, message);
                }
                crawlUrl.setMessages(messageMap);
            }
            return updateCrawlUrlFromTodoUrls(crawlUrl);
        }
        return OperationStatus.NOTFOUND;
    }

    //更新相应todoUrls中的weight值
    public OperationStatus updateWeight(String url, int weight) {
        CrawlUrl crawlUrl = getCrawlUrlFromTodoUrls(url);
        if (crawlUrl != null) {
            crawlUrl.setWeight(weight);
            return updateCrawlUrlFromTodoUrls(crawlUrl);
        }
        return OperationStatus.NOTFOUND;
    }

    //根据相对差值,更新相应todoUrls中的weight值
    public OperationStatus updateWeightByRelativeValue(String url, int weightRelativeValue) {
        CrawlUrl crawlUrl = getCrawlUrlFromTodoUrls(url);
        if (crawlUrl != null) {
            crawlUrl.setWeight(crawlUrl.getWeight() + weightRelativeValue);
            return updateCrawlUrlFromTodoUrls(crawlUrl);
        }
        return OperationStatus.NOTFOUND;
    }

    //更新相应uniUrls中的simHash值
    public OperationStatus updateSimHash(String url, BigInteger simHash) {
        CrawlUrl crawlUrl = getCrawlUrlFromUniUrls(url);
        if (crawlUrl == null) return OperationStatus.NOTFOUND;
        crawlUrl.setSimHash(simHash);
        return updateCrawlUrlFromUniUrls(crawlUrl);
    }

    //清除todoUrls,uniUrls所有条目
    public void clearDb() {
        Transaction txn = null;
        Cursor cursor = null;
        DatabaseEntry key = new DatabaseEntry();
        DatabaseEntry value = new DatabaseEntry();
        OperationStatus retVal = null;

        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            //清除todoUrls
            cursor = db.todoUrls.openCursor(txn, null);
            retVal = cursor.getFirst(key, value, LockMode.DEFAULT);
            for (; ; ) {
                if (retVal == OperationStatus.SUCCESS) {
                    cursor.delete();
                } else break;
                retVal = cursor.getNext(key, value, LockMode.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) cursor.close();
            if (txn != null) txn.commit();
        }

        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            //清除uniUrls
            cursor = db.uniUrls.openCursor(txn, null);
            retVal = cursor.getFirst(key, value, LockMode.DEFAULT);
            for (; ; ) {
                if (retVal == OperationStatus.SUCCESS) {
                    cursor.delete();
                } else break;
                retVal = cursor.getNext(key, value, LockMode.DEFAULT);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) cursor.close();
            if (txn != null) txn.commit();
        }

    }

    //从todoUrls移除url,并添加到uniUrls
    public OperationStatus settleUrl(String url, int statusCode) {
        Transaction txn = null;
        if (url == null) return OperationStatus.NOTFOUND;
        CrawlUrl crawlUrl = null;
        Cursor cursor = null;
        DatabaseEntry key = null;
        try {
            key = new DatabaseEntry(url.getBytes("utf-8"));
        } catch (UnsupportedEncodingException e) {
        }
        //是否存在于todoUrls
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry value = new DatabaseEntry();
            cursor = db.todoUrls.openCursor(txn, null);
            if (cursor.getSearchKey(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                crawlUrl = (CrawlUrl) db.serialBinding.entryToObject(value);
                crawlUrl.setStatusCode(statusCode);
                cursor.delete();
            } else return OperationStatus.NOTFOUND;
        } finally {
            if (cursor != null) cursor.close();
            if (txn != null) txn.commit();
        }
        //添加到uniUrls
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry value = new DatabaseEntry();
            cursor = db.uniUrls.openCursor(txn, null);
            db.serialBinding.objectToEntry(crawlUrl, value);
            return cursor.put(key, value);
        } finally {
            if (cursor != null) cursor.close();
            if (txn != null) txn.commit();
        }
    }

    //从todoUrls中获取weight最高的条目
    public String getNext() {
        CrawlUrl crawlUrl = getNextCrawlUrl();
        return crawlUrl == null ? null : crawlUrl.getUrl();
    }

    protected CrawlUrl getNextCrawlUrl() {
        Transaction txn = null;
        SecondaryCursor secCursor = null;
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry searchKey = new DatabaseEntry();
            DatabaseEntry foundKey = new DatabaseEntry();
            DatabaseEntry foundValue = new DatabaseEntry();
            secCursor = db.todoUrlsByWeight.openCursor(txn, null);
            OperationStatus retVal = secCursor.getLast(searchKey, foundKey, foundValue, LockMode.DEFAULT);
            if (retVal == OperationStatus.SUCCESS) {
                CrawlUrl crawlUrl = (CrawlUrl) db.serialBinding.entryToObject(foundValue);
                return crawlUrl;
            }
            return null;
        } finally {
            if (secCursor != null) secCursor.close();
            if (txn != null) txn.commit();
        }
    }

    //判断url是否在todoUrls中,若存在则返回相应的CrawlUrl,不存在返回null
    public CrawlUrl getCrawlUrlFromTodoUrls(String url) {
        Transaction txn = null;
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry key = new DatabaseEntry(url.getBytes("utf-8"));
            DatabaseEntry value = new DatabaseEntry();
            Cursor cursor = null;
            try {
                cursor = db.todoUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    CrawlUrl crawlUrl = (CrawlUrl) db.serialBinding.entryToObject(value);
                    return crawlUrl;
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    //判断url是否在uniUrls中,若存在则返回相应的CrawlUrl,不存在返回null
    public CrawlUrl getCrawlUrlFromUniUrls(String url) {
        Transaction txn = null;
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry key = new DatabaseEntry(url.getBytes("utf-8"));
            DatabaseEntry value = new DatabaseEntry();
            Cursor cursor = null;
            try {
                cursor = db.uniUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    CrawlUrl crawlUrl = (CrawlUrl) db.serialBinding.entryToObject(value);
                    return crawlUrl;
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    //更新todoUrls中的CrawlUrl
    private OperationStatus updateCrawlUrlFromTodoUrls(CrawlUrl crawlUrl) {
        Transaction txn = null;
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry key = new DatabaseEntry(crawlUrl.getUrl().getBytes("utf-8"));
            DatabaseEntry value = new DatabaseEntry();
            Cursor cursor = null;
            try {
                cursor = db.todoUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    db.serialBinding.objectToEntry(crawlUrl, value);
                    return cursor.putCurrent(value);
                } else return OperationStatus.NOTFOUND;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return OperationStatus.NOTFOUND;
        }
    }

    //更新uniUrls中的CrawlUrl
    private OperationStatus updateCrawlUrlFromUniUrls(CrawlUrl crawlUrl) {
        Transaction txn = null;
        try {
            txn = db.env.beginTransaction(null, db.txnConf);
            DatabaseEntry key = new DatabaseEntry(crawlUrl.getUrl().getBytes("utf-8"));
            DatabaseEntry value = new DatabaseEntry();
            Cursor cursor = null;
            try {
                cursor = db.uniUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, value, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
                    db.serialBinding.objectToEntry(crawlUrl, value);
                    return cursor.putCurrent(value);
                } else return OperationStatus.NOTFOUND;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return OperationStatus.NOTFOUND;
        }
    }

    public void describe(boolean describeTodo, boolean describeUni) {
        db.describe(describeTodo, describeUni);
    }

    //添加一个url至todoUrls,深度为urlReferedTo+1,hasDepthRestriction时有深度限制
    public OperationStatus putUrl(String url) {
        return putUrl(new CrawlUrl(url), null, true);
    }

    public OperationStatus putUrl(String url, String urlReferedTo) {
        return putUrl(new CrawlUrl(url), urlReferedTo, true);
    }

    public OperationStatus putUrl(String url, String urlReferedTo, boolean hasDepthRestriction) {
        return putUrl(new CrawlUrl(url), urlReferedTo, hasDepthRestriction);
    }

    private OperationStatus putUrl(CrawlUrl crawlUrl) {
        return putUrl(crawlUrl, null, true);
    }

    private OperationStatus putUrl(CrawlUrl crawlUrl, String urlReferedTo, boolean hasDepthRestriction) {
        Transaction txn = null;
        try {
            DatabaseEntry key = new DatabaseEntry(crawlUrl.getUrl().getBytes("utf-8"));
            DatabaseEntry value = new DatabaseEntry();
            db.serialBinding.objectToEntry(crawlUrl, value);
            DatabaseEntry searchedValue = new DatabaseEntry();
            Cursor cursor = null;
            //是否存在于uniUrls
            try {
                txn = db.env.beginTransaction(null, db.txnConf);
                cursor = db.uniUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, searchedValue, LockMode.DEFAULT) != OperationStatus.NOTFOUND) {
//					Logger.log("已存在于uniUrls url = "+crawlUrl.getUrl()); //
                    if (urlReferedTo != null) {
                        CrawlUrl crawlUrlSearched = (CrawlUrl) db.serialBinding.entryToObject(searchedValue);
                        crawlUrlSearched.getUrlsReferedTo().add(urlReferedTo);
                        db.serialBinding.objectToEntry(crawlUrlSearched, searchedValue);
                        cursor.putCurrent(searchedValue);
                    }
                    return OperationStatus.KEYEXIST;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
            //添加至todoUrls
            try {
                txn = db.env.beginTransaction(null, db.txnConf);
                cursor = db.todoUrls.openCursor(txn, null);
                if (cursor.getSearchKey(key, searchedValue, LockMode.DEFAULT) != OperationStatus.NOTFOUND) {
//					Logger.log("已存在于todoUrls url = "+crawlUrl.getUrl()); //
                    if (urlReferedTo != null) {
                        CrawlUrl crawlUrlSearched = (CrawlUrl) db.serialBinding.entryToObject(searchedValue);
                        crawlUrlSearched.getUrlsReferedTo().add(urlReferedTo);
                        db.serialBinding.objectToEntry(crawlUrlSearched, searchedValue);
                        cursor.putCurrent(searchedValue);
                    }
                    return OperationStatus.KEYEXIST;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
            //添加url
            try {
                txn = db.env.beginTransaction(null, db.txnConf);
                //当前url是第一次添加,查找urlReferedTo的深度,并设置当前深度+1
                if (urlReferedTo != null) {
                    CrawlUrl sourceCrawlUrl = getCrawlUrlFromUniUrls(urlReferedTo);
                    if (sourceCrawlUrl != null) {
                        if (sourceCrawlUrl.getDepth() + 1 > maxDepth
                                && hasDepthRestriction)
                            return OperationStatus.KEYEXIST; //超过搜索深度
                        crawlUrl.setDepth(sourceCrawlUrl.getDepth() + 1);
                        db.serialBinding.objectToEntry(crawlUrl, value);
                    }
                }
                cursor = db.todoUrls.openCursor(txn, null);
//				Logger.log("添加至todoUrls url = "+crawlUrl.getUrl()); //
                cursor.put(key, value);
                return OperationStatus.SUCCESS;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if (cursor != null) cursor.close();
                if (txn != null) txn.commit();
            }
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return OperationStatus.KEYEMPTY;
        }
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }


}
