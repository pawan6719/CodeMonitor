package com.codekitchen.codereviewer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ReviewPayload {

    private final Map<String, Object> payload;

    public ReviewPayload(Map<String, Object> payload) {
        this.payload = payload == null ? Collections.emptyMap() : payload;
    }

    public Map<String, Object> getPayload() {
        return payload;
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
        return asString(getRepository().get("full_name"));
    }

    public String getRepositoryName() {
        return asString(getRepository().get("name"));
    }

    public String getRepositoryOwnerLogin() {
        Map<String, Object> repository = getRepository();
        Map<String, Object> owner = asMap(repository.get("owner"));
        return asString(owner.get("login"));
    }

    public Map<String, Object> getPullRequest() {
        return asMap(payload.get("pull_request"));
    }

    public Integer getPullRequestNumber() {
        return asInteger(getPullRequest().get("number"));
    }

    public String getPullRequestTitle() {
        return asString(getPullRequest().get("title"));
    }

    public String getPullRequestBody() {
        return asString(getPullRequest().get("body"));
    }

    public String getPullRequestHtmlUrl() {
        return asString(getPullRequest().get("html_url"));
    }

    public String getPullRequestState() {
        return asString(getPullRequest().get("state"));
    }

    public String getHeadSha() {
        return asString(getPullRequest().get("head") == null ? null : ((Map<String, Object>) getPullRequest().get("head")).get("sha"));
    }

    public String getBaseSha() {
        return asString(getPullRequest().get("base") == null ? null : ((Map<String, Object>) getPullRequest().get("base")).get("sha"));
    }

    public String getCompareUrl() {
        return asString(payload.get("compare"));
    }

    public List<Map<String, Object>> getCommits() {
        return asMapList(payload.get("commits"));
    }

    public List<String> getModifiedFiles() {
        Map<String, Object> headCommit = asMap(payload.get("head_commit"));
        return asStringList(headCommit.get("modified"));
    }

    public Map<String, Object> getSender() {
        return asMap(payload.get("sender"));
    }

    public String getSenderLogin() {
        return asString(getSender().get("login"));
    }

    public Map<String, Object> getPusher() {
        return asMap(payload.get("pusher"));
    }

    public String getPusherName() {
        return asString(getPusher().get("name"));
    }

    public String getPusherEmail() {
        return asString(getPusher().get("email"));
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }
        return Collections.emptyMap();
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
