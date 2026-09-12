package com.codekitchen.codereviewer.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ReviewPayload {

    private Map<String, Object> payload = new HashMap<>();

    public ReviewPayload() {
    }

    public ReviewPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new HashMap<>() : payload;
    }

    @JsonAnyGetter
    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload == null ? new HashMap<>() : payload;
    }

    @JsonAnySetter
    public void setDynamicField(String key, Object value) {
        this.payload.put(key, value);
    }

    public String getAction() {
        return asString(payload.get("action"));
    }

    public String getRef() {
        return asString(payload.get("ref"));
    }

    public String getBefore() {
        return asString(payload.get("before"));
    }

    public String getAfter() {
        return asString(payload.get("after"));
    }

    public String getBaseRef() {
        return asString(payload.get("base_ref"));
    }

    public Map<String, Object> getRepository() {
        return asMap(payload.get("repository"));
    }

    public String getRepositoryFullName() {
        Map<String, Object> repository = getRepository();
        return repository == null ? null : asString(repository.get("full_name"));
    }

    public String getRepositoryName() {
        Map<String, Object> repository = getRepository();
        return repository == null ? null : asString(repository.get("name"));
    }

    public String getRepositoryOwnerLogin() {
        Map<String, Object> repository = getRepository();
        if (repository == null) {
            return null;
        }
        Map<String, Object> owner = asMap(repository.get("owner"));
        return owner == null ? null : asString(owner.get("login"));
    }

    public Map<String, Object> getPullRequest() {
        return asMap(payload.get("pull_request"));
    }

    public Integer getPullRequestNumber() {
        Map<String, Object> pullRequest = getPullRequest();
        return pullRequest == null ? null : asInteger(pullRequest.get("number"));
    }

    public String getPullRequestTitle() {
        Map<String, Object> pullRequest = getPullRequest();
        return pullRequest == null ? null : asString(pullRequest.get("title"));
    }

    public String getPullRequestBody() {
        Map<String, Object> pullRequest = getPullRequest();
        return pullRequest == null ? null : asString(pullRequest.get("body"));
    }

    public String getPullRequestHtmlUrl() {
        Map<String, Object> pullRequest = getPullRequest();
        return pullRequest == null ? null : asString(pullRequest.get("html_url"));
    }

    public String getPullRequestState() {
        Map<String, Object> pullRequest = getPullRequest();
        return pullRequest == null ? null : asString(pullRequest.get("state"));
    }

    public String getHeadSha() {
        Map<String, Object> pullRequest = getPullRequest();
        if (pullRequest == null || pullRequest.get("head") == null) {
            return null;
        }
        Map<String, Object> head = asMap(pullRequest.get("head"));
        return head == null ? null : asString(head.get("sha"));
    }

    public String getBaseSha() {
        Map<String, Object> pullRequest = getPullRequest();
        if (pullRequest == null || pullRequest.get("base") == null) {
            return null;
        }
        Map<String, Object> base = asMap(pullRequest.get("base"));
        return base == null ? null : asString(base.get("sha"));
    }

    public String getCompareUrl() {
        return asString(payload.get("compare"));
    }

    public List<Map<String, Object>> getCommits() {
        return asMapList(payload.get("commits"));
    }

    public List<String> getModifiedFiles() {
        Map<String, Object> headCommit = asMap(payload.get("head_commit"));
        if (headCommit == null) {
            return new ArrayList<>();
        }
        return asStringList(headCommit.get("modified"));
    }

    public Map<String, Object> getSender() {
        return asMap(payload.get("sender"));
    }

    public String getSenderLogin() {
        Map<String, Object> sender = getSender();
        return sender == null ? null : asString(sender.get("login"));
    }

    public Map<String, Object> getPusher() {
        return asMap(payload.get("pusher"));
    }

    public String getPusherName() {
        Map<String, Object> pusher = getPusher();
        return pusher == null ? null : asString(pusher.get("name"));
    }

    public String getPusherEmail() {
        Map<String, Object> pusher = getPusher();
        return pusher == null ? null : asString(pusher.get("email"));
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return new HashMap<>();
    }

    private static List<Map<String, Object>> asMapList(Object value) {
        if (value instanceof List<?> listValue) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : listValue) {
                if (item instanceof Map<?, ?> mapItem) {
                    result.add((Map<String, Object>) mapItem);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static List<String> asStringList(Object value) {
        if (value instanceof List<?> listValue) {
            List<String> result = new ArrayList<>();
            for (Object item : listValue) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.valueOf(stringValue);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
