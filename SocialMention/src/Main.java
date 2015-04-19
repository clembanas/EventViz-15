import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import parser.SocialMentionParser;
import data.SocialMentionData;

public class Main {

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

		// final String url1 = SocialMentionAccessor.createQueryURL(queryTerms,
		// searchType, location);
		// System.out.println(url1);

		// TODO: fertig geladenes HTML file auf HDD speichern oder Aufruf aus
		// javacode...

		// final String url =
		// "http://socialmention.com/search?q=wacken+festival&t=all&btnG=Search";
		// final Document doc = Jsoup.connect(url).timeout(100 *
		// 1000).userAgent("Mozilla").ignoreContentType(true).get();
		// System.out.println(doc);

		final File input = new File("html-files/untitled.html");
		final String baseURI = "http://socialmention.com/";
		try {
			final SocialMentionData data = SocialMentionParser.parseHTMLContent(input, baseURI);
			System.out.println("parsing completed!");
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}