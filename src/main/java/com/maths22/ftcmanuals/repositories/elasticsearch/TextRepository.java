package com.maths22.ftcmanuals.repositories.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maths22.ftcmanuals.models.Definition;
import com.maths22.ftcmanuals.models.ForumPost;
import com.maths22.ftcmanuals.models.Rule;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;

@Component
public class TextRepository {

    private final ElasticsearchTemplate elasticsearchTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public TextRepository(ElasticsearchTemplate elasticsearchTemplate, ObjectMapper objectMapper) {
        this.elasticsearchTemplate = elasticsearchTemplate;
        this.objectMapper = objectMapper;
    }

    public List<?> search(String text, Pageable pageable) {
        QueryBuilder qb = multiMatchQuery(text)
                .field("number", 4)
                .field("title", 3)
                .field("body")
                .field("question")
                .field("answer")
                .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
                .tieBreaker(0.3f);

        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(qb)
                .withIndices("ftc-manuals-texts")
                .withPageable(pageable)
                .build();

        return elasticsearchTemplate.query(searchQuery, searchResponse -> {
            List<Object> ret = new ArrayList<>();
            searchResponse.getHits().iterator().forEachRemaining((hit) -> {
                Class<?> type;
                switch (hit.getType()) {
                    case "rule":
                        type = Rule.class;
                        break;
                    case "definition":
                        type = Definition.class;
                        break;
                    case "forum-post":
                        type = ForumPost.class;
                        break;
                    default:
                        //Unsupported type
                        //TODO log
                        return;
                }

                try {
                    ret.add(objectMapper.readValue(hit.getSourceAsString(), type));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return ret;
        });
    }
}
