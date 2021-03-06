package com.budilov.db

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.budilov.Properties
import com.budilov.pojo.PictureItem
import io.searchbox.client.JestClient
import io.searchbox.client.JestClientFactory
import io.searchbox.client.config.HttpClientConfig
import io.searchbox.core.Delete
import io.searchbox.core.Get
import io.searchbox.core.Index
import io.searchbox.core.Search
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import vc.inreach.aws.request.AWSSigner
import vc.inreach.aws.request.AWSSigningRequestInterceptor
import java.time.LocalDateTime
import java.time.ZoneOffset


/**
 * Created by Vladimir Budilov
 *
 * Simple DAO implementation for ElasticSearch
 *
 */

class ESPictureService : DBPictureService {

    private val client: JestClient

    private val _DEFAULT_INDEX = "pictures"

    init {
        val clock = { LocalDateTime.now(ZoneOffset.UTC) }

        // Sig4
        val awsSigner = AWSSigner(EnvironmentVariableCredentialsProvider(), Properties._REGION, "es", clock)

        // Adding a request interceptor to sign the ES requests with Sig4
        val factory = getJestFactory(AWSSigningRequestInterceptor(awsSigner))

        factory.setHttpClientConfig(HttpClientConfig.Builder(Properties._ES_SERVICE_URL)
                .multiThreaded(true)
                .build())

        client = factory.`object`
    }

    /**
     * Return one PictureItem
     *
     */
    override fun get(userId: String, docId: String): PictureItem {
        val get = Get.Builder(_DEFAULT_INDEX, docId).type(userId).build()

        val result = client.execute(get)

        return result.getSourceAsObject(PictureItem::class.java)
    }

    /**
     * Delete a picture from ES
     */
    override fun delete(userId: String, item: PictureItem): Boolean {

        val delete = client.execute(Delete.Builder(item.id)
                .index(_DEFAULT_INDEX)
                .type(userId)
                .build())

        return delete.isSucceeded
    }

    /**
     * Search for pictures.
     *
     * returns List<PictureItem>
     */
    override fun search(userId: String, query: String): List<PictureItem> {
        var matchAllQuery = ""
        var search: Search? = null

        if(query.contains(Properties._BUCKET_URL)) {
            matchAllQuery = getMatchAllQueryS3BucketUrl(query)
            println("query: " + query)
            println()
            search = Search.Builder(matchAllQuery)
                // multiple index or types can be added.
                .addIndex(_DEFAULT_INDEX)
                .addType(userId)
                .build()
            println("reached S3 bucket URL")
            println()
        }
        else if(query.contains(userId)) {
            matchAllQuery = getMatchAllQueryUserId(query)
            search = Search.Builder(matchAllQuery)
                // multiple index or types can be added.
                .addIndex(_DEFAULT_INDEX)
                .addType(userId)
                .build()
            println("reached userID")
            println()
        }
        else {
            matchAllQuery = getMatchAllQueryLabels(query)
            search = Search.Builder(matchAllQuery)
                // multiple index or types can be added.
                .addIndex(_DEFAULT_INDEX)
                .addType(userId)
                .build()
            println("reached default")
            println()
        }

        val result = client.execute(search)

        return result.getSourceAsObjectList(PictureItem::class.java)
    }

    /**
     * Add a picture
     *
     * For now it always returns true
     */
    override fun add(userId: String, item: PictureItem): Boolean {
        println("entered fun add")
        println()
        val index = Index.Builder(item).index(_DEFAULT_INDEX).type(userId).build()
        println("properly assigned index")
        println()
        val result = client.execute(index)
        println("es result: " + result)
        return result.isSucceeded
    }

    private fun getMatchAllQueryLabels(labels: String, pageSize: Int = 30): String {
        return """{"query": { "match": { "labels": "$labels" } }, "size":$pageSize}"""
    }

    private fun getMatchAllQueryS3BucketUrl(s3BucketUrl: String, pageSize: Int = 30): String {
        return """{"query": { "match": { "s3BucketUrl": { "query": "$s3BucketUrl", "cutoff_frequency": 0 } } }, "size":$pageSize}"""
    }

    private fun getMatchAllQueryUserId(userId: String, pageSize: Int = 30): String {
        return """{"query": { "match_all": {} }, "size":$pageSize}"""
    }

    private fun getJestFactory(requestInterceptor: AWSSigningRequestInterceptor): JestClientFactory {
        return object : JestClientFactory() {
            override fun configureHttpClient(builder: HttpClientBuilder): HttpClientBuilder {
                builder.addInterceptorLast(requestInterceptor)
                return builder
            }

            override fun configureHttpClient(builder: HttpAsyncClientBuilder): HttpAsyncClientBuilder {
                builder.addInterceptorLast(requestInterceptor)
                return builder
            }
        }
    }
}