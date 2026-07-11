package com.huixiang.service;

import java.util.List;

public interface SearchService {

    List<String> hotKeywords();

    void recordKeyword(String keyword);
}
