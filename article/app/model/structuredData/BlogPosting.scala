package model.structuredData

import common.LinkTo
import model.Article
import model.liveblog.BodyBlock
import model.ImageElement
import model.ImageBlockElement
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import views.support.Format

object BlogPosting {

  def zulu(date: DateTime)(implicit request: RequestHeader): String = Format(date, "yyyy-MM-dd'T'HH:mm:ssZ")

  def apply(blog: Article, block: BodyBlock)(implicit request: RequestHeader): JsValue = {

    def blockFirstPublishedDate(block: BodyBlock) = block.firstPublishedDate match {
      case Some(date) => zulu(date)
      case None => zulu(blog.trail.webPublicationDate)
    }
    
    def blockLastModifiedDate(block: BodyBlock) = block.lastModifiedDate match {
      case Some(date) => zulu(date)
      case None => zulu(blog.metadata.lastModified)
    }

    def blockBody(block: BodyBlock): String = {
      if(block.bodyTextSummary.length < 1000){
        block.bodyTextSummary
      } else {
        block.bodyTextSummary.substring(0, 1000)
      }
    }

    def blockAuthor(blog: Article, block: BodyBlock): JsValue = {

      val name = block.contributors.headOption match {
        case Some(id) => blog.tags.tags.find(_.id == s"profile/$id").map{ contributorTag =>
          contributorTag.name
        }
        case None => None
      }

      name match {
        case Some(thing) => Json.obj("@type" -> "Person", "name" -> thing)
        case None => Json.obj("@id" -> "https://www.theguardian.com#publisher")
      }

    }
    
    def blockImage(blog: Article, block: BodyBlock): JsValue = {
       block.elements.collect {
         case ImageBlockElement(media, data, showCredit) => Image(ImageElement(ElementProperties(null, 0), media)))
       }.headOption.getOrElse{
         blog.elements.mainPicture.map { picture => Image(picture) }
       }
      }
    }
    
    
    
    Json.obj(
      "@type" -> "BlogPosting",
      "headline" -> block.title.getOrElse[String](blog.trail.headline),
      "author" -> blockAuthor(blog, block),
      "publisher" -> Json.obj("@id" -> "https://www.theguardian.com#publisher"),
      "url" -> LinkTo{blog.metadata.url+"?page=with:block-"+block.id+"#block-"+block.id},
      /* Schema.org -- Date of first broadcast/publication */
      "datePublished" -> blockFirstPublishedDate(block),
      /*  Schema.org -- The date on which the CreativeWork was most recently modified or when the item's entry was modified within a DataFeed */
      "dateModified" -> blockLastModifiedDate(block),
      "image" -> blockImage(block),
      "articleBody" -> blockBody(block)
    )

  }


}
