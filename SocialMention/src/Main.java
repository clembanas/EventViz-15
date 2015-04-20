import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import parser.SocialMentionParser;
import data.SocialMentionData;

public class Main {

	@SuppressWarnings({ "unused", "serial" })
	public static void main(final String[] args) {
		final List<String> queryTerms = new ArrayList<String>() {
			{
				add("iphone");
				add("apps");
			}
		};
		final List<String> searchType = new ArrayList<String>() {
			{
				add("blogs");
				add("microblogs");
			}
		};
		final String location = "usa";

		try {
			final String url = "http://socialmention.com/search?q=coachella+festival&t=all&btnG=Search";

			Element test_element = null;
			Document htmlContent = null;
			while (test_element == null) {
				htmlContent = Jsoup.connect(url).timeout(100 * 1000).userAgent("Mozilla").ignoreContentType(true).followRedirects(true).get();
				test_element = htmlContent.getElementById("score_strength");
			}

			final SocialMentionData data = SocialMentionParser.parseHTMLContent(htmlContent);
			System.out.println("parsing completed!");
			System.out.println(data);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}