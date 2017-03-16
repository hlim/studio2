import org.craftercms.sites.editorial.SearchHelper

def segment = null

if (profile) {
    segment = profile.attributes.segment
    if (segment == "unknown") {
      segment = null
    }
}

def searchHelper = new SearchHelper(searchService, urlTransformationService)
def articles = searchHelper.searchArticles(true, null, segment)

templateModel.articles = articles