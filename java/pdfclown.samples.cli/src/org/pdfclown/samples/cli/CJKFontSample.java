package org.pdfclown.samples.cli;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;

import org.pdfclown.documents.Document;
import org.pdfclown.documents.Page;
import org.pdfclown.documents.contents.composition.BlockComposer;
import org.pdfclown.documents.contents.composition.PrimitiveComposer;
import org.pdfclown.documents.contents.composition.XAlignmentEnum;
import org.pdfclown.documents.contents.composition.YAlignmentEnum;
import org.pdfclown.documents.contents.fonts.Font;
import org.pdfclown.documents.contents.fonts.CJKFont;
import org.pdfclown.files.File;

/**
  This sample demonstrates the PDF Clown's <b>support to Unicode-compliant fonts</b>.

  @author Keiji Suzuki (https://github.com/zuki/PDFClown)
  @author Stefano Chizzolini (http://www.stefanochizzolini.it)
  @version 0.1.2, 2014-02-18
*/
public class CJKFontSample
  extends Sample
{
  private static final float Margin = 36;

  @Override
  public void run(
    )
  {
    // 1. Instantiate a new PDF file!
    File file = new File();
    Document document = file.getDocument();

    // 2. Insert the contents into the document!
    populate(document);

    // 3. Serialize the PDF file!
    serialize(file, "CJKFont", "using CJK fonts", "CJKFont");
  }

  /**
    Populates a PDF file with contents.
  */
  private void populate(
    Document document
    )
  {
    // 1. Add the page to the document!
    Page page = new Page(document); // Instantiates the page inside the document context.
    document.getPages().add(page); // Puts the page in the pages collection.

    // 2.1. Create a content composer for the page!
    PrimitiveComposer composer = new PrimitiveComposer(page);

    // 2.2. Create a block composer!
    BlockComposer blockComposer = new BlockComposer(composer);

    // 3. Inserting contents...
    // Define the font to use!
    Font[] fonts = new Font[8];
    fonts[0] = CJKFont.get(document, "cn", "sans");
    fonts[1] = CJKFont.get(document, "cn", "serif");
    fonts[2] = CJKFont.get(document, "tw", "sans");
    fonts[3] = CJKFont.get(document, "tw", "serif");
    fonts[4] = CJKFont.get(document, "ja", "sans");
    fonts[5] = CJKFont.get(document, "ja", "serif");
    fonts[6] = CJKFont.get(document, "ko", "sans");
    fonts[7] = CJKFont.get(document, "ko", "serif");

    // Define the paragraph break size!
    Dimension breakSize = new Dimension(0,10);
    // Define the text to show!
    String[] titles = new String[]
      {
        "\u4e2d\u6587\uff08\u7c97\u9ed1\u5b57\u4f53\uff09",
        "\u4e2d\u6587(\u660e\u671d\u4f53)",
        "\u4e2d\u6587\uff08\u7c97\u9ed1\u5b57\u9ad4\uff09",
        "\u4e2d\u6587(\u660e\u671d\u9ad4)",
        "\u65e5\u672c\u8a9e\uff08\u30b4\u30b7\u30c3\u30af\u4f53\uff09",
        "\u65e5\u672c\u8a9e\uff08\u660e\u671d\u4f53\uff09",
        "\ud55c\uad6d\uc5b4(\uace0\ub515\uccb4)",
        "\ud55c\uad6d\uc5b4(\uba85\uc870\uccb4)",
      };
    String[] bodies = new String[]
      {
        "\u521d\u6b21\u89c1\u9762\u3002\u6211\u662f\u6765\u81ea\u5317\u4eac\u7684\u7559\u5b66\u751f\u3002",
        "\u521d\u6b21\u89c1\u9762\u3002\u6211\u662f\u6765\u81ea\u5317\u4eac\u7684\u7559\u5b66\u751f\u3002",
        "\u521d\u6b21\u898b\u9762\u3002\u6211\u662f\u4f86\u81ea\u53f0\u5317\u7684\u7559\u5b78\u751f\u3002",
        "\u521d\u6b21\u898b\u9762\u3002\u6211\u662f\u4f86\u81ea\u53f0\u5317\u7684\u7559\u5b78\u751f\u3002",
        "\u306f\u3058\u3081\u307e\u3057\u3066\u3002\u79c1\u306f\u6771\u4eac\u304b\u3089\u304d\u305f\u7559\u5b66\u751f\u3067\u3059\u3002",
        "\u306f\u3058\u3081\u307e\u3057\u3066\u3002\u79c1\u306f\u6771\u4eac\u304b\u3089\u304d\u305f\u7559\u5b66\u751f\u3067\u3059\u3002",
        "\ucc98\uc74c \ubd59\uaca0\uc2b5\ub2c8\ub2e4. \uc800\ub294 \uc11c\uc6b8\uc5d0\uc11c \uc628 \uc720\ud559\uc0dd\uc785\ub2c8\ub2e4..",
        "\ucc98\uc74c \ubd59\uaca0\uc2b5\ub2c8\ub2e4. \uc800\ub294 \uc11c\uc6b8\uc5d0\uc11c \uc628 \uc720\ud559\uc0dd\uc785\ub2c8\ub2e4.",
      };
    // Begin the content block!
    blockComposer.begin(
      new Rectangle2D.Double(
        Margin,
        Margin,
        page.getSize().getWidth() - Margin * 2,
        page.getSize().getHeight() - Margin * 2
        ),
      XAlignmentEnum.Justify,
      YAlignmentEnum.Top
      );
    for(
      int index = 0,
        length = titles.length;
      index < length;
      index++
      )
    {
      composer.setFont(fonts[index],12);
      blockComposer.showText(titles[index]);
      blockComposer.showBreak(XAlignmentEnum.Right);

      composer.setFont(fonts[index],12);
      blockComposer.showText(bodies[index]);
      blockComposer.showBreak(XAlignmentEnum.Justify);
    }
    // End the content block!
    blockComposer.end();

    // 4. Flush the contents into the page!
    composer.flush();
  }
}
