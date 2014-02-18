/*******************************************************************
PDFClown CJKFonts
Copyright (c) 2013 Web Lite Solutions Corp.
All rights reserved.

* 
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*********************************************************************/
package org.pdfclown.documents.contents.fonts;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.*;

import org.pdfclown.documents.Document;
import org.pdfclown.documents.contents.fonts.Font;
import org.pdfclown.objects.PdfArray;
import org.pdfclown.objects.PdfDictionary;
import org.pdfclown.objects.PdfDirectObject;
import org.pdfclown.objects.PdfInteger;
import org.pdfclown.objects.PdfName;
import org.pdfclown.objects.PdfReference;
import org.pdfclown.objects.PdfString;
import org.pdfclown.objects.Rectangle;

/**
 * <p>A Font for PDFClown that supports Chinese (Simplified & Traditional), Japanese,
 * and Korean characters.  It uses TCPDF's (http://www.tcpdf.org/) CID0 unicode fonts
 * for the font information (e.g. character widths).  It actually parses the PHP files
 * directly to get this information.</p>
 * <p>This could probably be adapted to parse font files directly but I found the 
 * PHP files more accessible at this point.</p>
 * 
 * <p>All of these languages specify that the ArialUnicodeMS font should be used
 * for rendering the Unicode text.  This requires that your PDF reader has this font
 * installed.  Typically, Adobe Reader will pop up with a message saying that some
 * fonts require the CJK language pack in order to work properly, then it will just download
 * the font kit and allow you to load the document.</p>
 * 
 * 
 * @author shannah
 * @author Keiji Suzuki
 */
public class CJKFont extends Font 
{

    
    /**
     * Flag to indicate whether the font has been loaded yet.
     */
    private boolean loaded = false;
    
    /**
     * The language: s: Simplified Chinese, t: Traditional Chinese, j: Japanese, k: Korean
     */
    private Map<String, String> info;

    /**
     * The type of the font.
     *    sans: sans-serif
     *    serif: serif
     */
    private String type;

    /**
     * The code of the language.
     *    cn: Simplified Chinese, 
     *    tw: Traditional Chinese,
     *    ja: Japanese,
     *    ko: Korean
     */
    private String lang;

    /**
     * The FontDescriptor that is returned by the getDescriptor() method.
     */
    private PdfDictionary descriptor;
    
    /**
     * The document to which this font has been added.
     */
    private Document document;

    /**
     * The CID0Font object.
     */
    private PdfDictionary cid0Font;

    /**
     * The mapping from unicode (UTF-16) to cid
     */
    private Map<Integer, Integer> uni2cid = null;

    /**
     * The mapping from cid to its font width
     */
    private Map<Integer, Integer> widthCache = null;

    /**
     * The default font width
     */
    private int defaultWidth = 1000;

    /**
     * The static variables to parse cmap files
     */
    private static final String BEGIN_CID_CHAR = "begincidchar";
    private static final String END_CID_CHAR = "endcidchar";
    private static final String BEGIN_CID_RANGE = "begincidrange";
    private static final String END_CID_RANGE = "endcidrange";

    private boolean inCidChar = false;
    private boolean inCidRange = false;

    private static Pattern CID_PATTERN = Pattern.compile("^<([0-9a-fA-F]+)> ([0-9]+)$");
    private static Pattern CID_RANGE_PATTERN = Pattern.compile("^<([0-9a-fA-F]+)> <([0-9a-fA-F]+)> ([0-9]+)$");

    /**
     * @return A font that can be used to render text in a document.
     * @param context
     * @param lang - lang code: "cn", "tw", "ja", "ko"
     * @param type - font type: "sans", "serif"
     * @return A specified CJK font
     *
     * @see loadChineseSimplifiedSans()
     * @see loadChineseSimplifiedSerif()
     * @see loadChineseTraditionalSans()
     * @see loadChineseTraditionalSerif()
     * @see loadJapaneseSans()
     * @see loadJapaneseSerif()
     * @see loadKoreanSans()
     * @see loadKoreanSerif()
     */
    public static CJKFont get(Document context, String lang, String type)
    {
        CJKFont f =  new CJKFont(context);
        f.lang = lang;
        f.type = type;
        f.load();
        return f;
    }
    
    /**
     * Loads a font that can be used to render chinese simplified.  
     * @param context
     * @return A Chinese simplified sans-serif font.
     */
    public static CJKFont loadChineseSimplifiedSans(Document context)
    {
        return get(context, "cn", "sans");
    }
    
    /**
     * Loads a font that can be used to render chinese simplified.
     * @param context
     * @return A Chinese simplified serif font.
     */
    public static CJKFont loadChineseSimplifiedSerif(Document context)
    {
        return get(context, "cn", "serif");
    }

    /**
     * Loads a Chinese traditional font.  
     * @param context
     * @return A Chinese traditional sans-serif font.
     */
    public static CJKFont loadChineseTraditionalSans(Document context)
    {
        return get(context, "tw", "sans");
    }
    
    /**
     * Loads a Chinese traditional font.  
     * @param context
     * @return A Chinese traditional serif font.
     */
    public static CJKFont loadChineseTraditionalSerif(Document context)
    {
        return get(context, "tw", "serif");
    }
    
    /**
     * Loads a Japanese font.
     * @return A Japanese sans-serif font.
     */
    public static CJKFont loadJapaneseSans(Document context){
        return get(context, "ja", "sans");
    }
    
    /**
     * Loads a Japanese font.
     * @param context
     * @return A Japanese serif font.
     */
    public static CJKFont loadJapaneseSerif(Document context){
        return get(context, "ja", "serif");
    }
    
    /**
     * Loads a Korean font.  
     * @param context
     * @return A Korean sans-serif font.
     */
    public static CJKFont loadKoreanSans(Document context)
    {
        return get(context, "ko", "sans");
    }

    /**
     * Loads a Korean font.  
     * @param context
     * @return A Korean erif font.
     */
    public static CJKFont loadKoreanSerif(Document context)
    {
        return get(context, "ko", "serif");
    }

    protected CJKFont(Document context){
        super(context);
        document = context;
    }

    @Override
    protected PdfDictionary getDescriptor() {
        if ( descriptor == null ){
            descriptor = new PdfDictionary();
        }
        return descriptor;
    }

    @Override
    protected void onLoad() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * load font
     */
    @Override
    protected void load(){
        if ( loaded ){
            return;
        }
        loaded = true;
        this.symbolic = false;
        
        info = getInfo();

        // Start out by populating the base font object with the main information 
        // about the font.
        this.getBaseDataObject().put(PdfName.Type, PdfName.Font);
        this.getBaseDataObject().put(PdfName.Subtype, PdfName.Type0);
        
        // Right now everything is based on ArialUnicodeMS.  We might change this
        // if more fonts are added later.
        this.getBaseDataObject().put(PdfName.BaseFont, new PdfName(info.get("BaseFont")));
        
        // We need to create a CID0Font a sa descendant of the base font tag.
        cid0Font = new PdfDictionary();
        cid0Font.put(PdfName.Type, PdfName.Font);
        cid0Font.put(PdfName.Subtype,  PdfName.CIDFontType0);
        cid0Font.put(PdfName.BaseFont, new PdfName(info.get("BaseFont")));
        cid0Font.put(new PdfName("DW"), new PdfInteger(Integer.parseInt(info.get("DW"))));
        cid0Font.put(PdfName.W, getWArray(info.get("W")));
        
        // Register the CID0 font with the document and add it as an indirect object
        // to the base font as its descendant.
        PdfReference cid0Ref = document.getFile().register(cid0Font);
        this.getBaseDataObject().put(PdfName.DescendantFonts, new PdfArray(cid0Ref));
        getBaseDataObject().put(PdfName.Encoding, new PdfName(info.get("Encoding")));
        getBaseDataObject().put(PdfName.BaseFont, new PdfName(info.get("BaseFont")));

        PdfDictionary dict = new PdfDictionary();
        cid0Font.put(PdfName.CIDSystemInfo, dict);
        dict.put(PdfName.Registry, new PdfString("Adobe"));
        dict.put(PdfName.Ordering, new PdfString(info.get("Ordering")));
        dict.put(PdfName.Supplement, new PdfInteger(Integer.parseInt(info.get("Supplement"))));
                        
        descriptor = getDescriptor();
        descriptor.put(PdfName.Type, PdfName.FontDescriptor);
        descriptor.put(PdfName.FontName, new PdfName(info.get("BaseFont")));
        descriptor.put(PdfName.Flags, new PdfInteger(Integer.parseInt(info.get("Flags"))));
        descriptor.put(PdfName.ItalicAngle, new PdfInteger(Integer.parseInt(info.get("ItalicAngle"))));
        descriptor.put(PdfName.Ascent, new PdfInteger(Integer.parseInt(info.get("Ascent"))));
        descriptor.put(PdfName.Descent, new PdfInteger(Integer.parseInt(info.get("Descent"))));
        descriptor.put(PdfName.CapHeight, new PdfInteger(Integer.parseInt(info.get("CapHeight"))));
        descriptor.put(PdfName.StemV, new PdfInteger(Integer.parseInt(info.get("StemV"))));

        String[] bbox = info.get("FontBBox").split(",");
        descriptor.put(PdfName.FontBBox,
            new Rectangle(
              new Point2D.Double(Double.parseDouble(bbox[0]), Double.parseDouble(bbox[1])),
              new Point2D.Double(Double.parseDouble(bbox[2]), Double.parseDouble(bbox[3]))
            ).getBaseDataObject()
        );
        
        PdfReference ref = document.getFile().register(descriptor);
        cid0Font.put(PdfName.FontDescriptor, ref);
    }

    @Override
    public int getWidth(String text)
    {
        int width = 0;
        for (int cp : toCodePointArray(text))
        {
            if (uni2cid.containsKey(cp))
            {
                width += getWidth(uni2cid.get(cp));
            }
        }
        return width;
    }

    @Override
    public int getWidth(char textChar)
    {
        int width = 0;

        int charCode = (int) textChar;
        if (uni2cid.containsKey(charCode))
        {
            width = getWidth(uni2cid.get(charCode));
        }

        return width;
    }

    public int getWidth( int charCode ) 
    {
        int width = defaultWidth;
        if (widthCache.containsKey(charCode)) 
        {
            width = widthCache.get(charCode);
        }
        return width;
    }

    @Override
    public byte[] encode(String text) 
    {
        return text.getBytes(Charset.forName("UTF-16BE"));
    }

    @Override
    public String decode(byte[] code) 
    {
        return new String(code, Charset.forName("UTF-16BE"));
    }

    private Map<String, String> getInfo()
    {
        if ("cn".equals(lang)) {
            if ("sans".equals(type)) {
                return getCNSansInfo();
            } else if ("serif".equals(type)) {
                return getCNSerifInfo();
            }
        } else if ("tw".equals(lang)) {
            if ("sans".equals(type)) {
                return getTWSansInfo();
            } else if ("serif".equals(type)) {
                return getTWSerifInfo();
            }
        } else if ("ja".equals(lang)) {
            if ("sans".equals(type)) {
                return getJASansInfo();
            } else if ("serif".equals(type)) {
                return getJASerifInfo();
            }
        } else if ("ko".equals(lang)) {
            if ("sans".equals(type)) {
                return getKOSansInfo();
            } else if ("serif".equals(type)) {
                return getKOSerifInfo();
            }
        }

        return getJASansInfo();
    }

    private Map<String, String> getJASansInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "KozGoPr6N-Medium");
        info.put("Ordering", "Japan1");
        info.put("Supplement", "6");
        info.put("DW", "1000");
        info.put("Encoding", "UniJIS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "763");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-538,-378,1254,1418");
        info.put("ItalicAngle", "0");
        info.put("StemV", "116");
        info.put("W", "1 [224 266 392 551 562 883 677 213] 9 10 322 11 [470 677 247 343 245 370] 17 26 562 27 [245 247] 29 31 677 32 [447 808 661 602 610 708 535 528 689 703 275 404 602 514 871 708 727 585 727 595 539 541 696 619 922 612 591 584 322 562 322 677 568 340 532 612 475 608 543 332 603 601 265 276 524 264 901 601 590 612 607 367 433 369 597 527 800 511 518 468 321 273 321 341 241 362 241 273 677 266] 102 103 562 104 [456 562 571 562 416 472] 110 111 283 112 [587 588 568] 115 116 545 117 [247 561 330 239 418 416 472 1136 1288 447] 127 132 340 133 [455] 134 137 340 138 [1136 857 384 519 727 952 398 834 264 275 590 918 605 677 769 677 473 361 677 347 340 599 284] 161 163 845 164 169 661 170 [610] 171 174 535 175 178 275 179 [715 708] 181 185 727 186 [677] 187 190 696 191 [591 584] 193 198 532 199 [475] 200 203 543 204 207 264 208 [584 601] 210 214 590 215 [677] 216 219 597 220 [518 612 518 539 591 584 446 433 683 468 562] 231 632 500 8718 8719 500 9354 [562 753 245 436 650] 9359 9360 909 9361 [532 264 597 543 590 661 275 696 535 727] 9371 9376 845 9377 [375 387 345 369 328 366 364 375 284 347 340 387 345 369 328 366 364 661] 9395 9397 535 9398 9400 275 9401 9402 727 9403 9405 696 9406 [532] 9407 9409 543 9410 9412 264 9413 9414 590 9415 9417 597 9418 9420 596 9421 9422 834 9423 9425 475 9426 9428 543 9429 9431 759 9432 9434 478 9435 [276 602 589] 9438 9440 527 9441 [509 465 280 197 270 359 545 546 869 664 188] 9452 9453 322 9454 [469 676 249 343 249 359 545] 9461 9469 546 9470 9471 249 9472 9474 676 9475 [429 791 620 573 581 688 511 504 665 681 269 395 572 477 844 677 709 555 709 573 509 515 663 593 898 565 562 557 322 546 322 676 567 427 566 571 447 570 498 320 567 577 258 254 507 257 870 577 564 572 568 359 409 343 572 500 767 486 487 448 322 245 322 427 228 363 228 245 676 269] 9545 9546 546 9547 [442 546 559 546 404 465] 9553 9554 275 9555 9556 562 9557 [568 533 534 249 550 326 228] 9564 9565 404 9566 [464 1136 1250 429] 9570 9575 427 9576 [423] 9577 9580 427 9581 [1136 835 396 492 709 923 388 781 258 270 567 858 592 677 765 677 443 361 677 358 354 573 343] 9604 9606 840 9607 9612 620 9613 [581] 9614 9617 511 9618 9621 269 9622 [700 677] 9624 9628 709 9629 [677] 9630 9633 663 9634 [562 555] 9636 9641 566 9642 [447] 9643 9646 498 9647 9650 258 9651 [562 577] 9653 9657 564 9658 [677] 9659 9662 572 9663 [487 573 487 509 562 557 446 409 735 448] 9673 9674 546 9675 [726 241 432 629] 9679 9680 868 9681 [566 258 572 498 564 620 269 663 511 709] 9691 9696 840 9697 [362 361 355 361 354 363 360 362 343 358 354 361 355 362 354 363 360 620] 9715 9717 511 9718 9720 269 9721 9722 709 9723 9725 663 9726 [566] 9727 9729 498 9730 9732 258 9733 9734 564 9735 9737 572 9738 9757 250 9758 9778 333 12063 12087 500 15455 [980] 15456 15458 676 15459 [750 676 865] 15464 15469 676 15470 15471 677 15472 15476 676 15477 [677] 15478 15479 676 15480 [649 652 840 890 675 540] 15486 15488 677 15489 15490 608 15491 [659 675 579 623] 15495 15496 676 15497 15498 737 15499 [658 750 725 676 372 609] 15505 15510 676 15511 [810 676 722 619 753 343 810 940 880 990 690 810 750 870 880 990] 15529 [910 940 750 870 960 980 810 910 890 950] 15539 15540 880 15541 15542 970 15543 [980 990 960 980 960 970 910 940 860] 15552 15553 990 15554 [920 940 840 870 970 960 980 990] 15562 15571 980 15575 [980] 15576 15577 990 15578 [880] 15579 15580 990 15581 [980] 15582 15583 880 15584 [980 890 990 790 870 850 790 880 990] 15593 15594 870 15595 15597 980 15598 [960 970] 15601 [910 850] 15604 [980 950] 15607 [960 780 930 760 920 800 910 850 980 820 950 920 950] 15620 15621 970 15622 [840 910 930 950 930 950] 15628 15629 980 15630 [950 960 940 970 980 990 920 950 890 940 980 990 820 940] 15644 15645 960 15646 [980 780 810 940 980 840 970 850] 15654 15656 990 15657 [870 910 900 920] 15661 15662 950 15663 15669 980 15670 [820 950 840 980 830 970 850 990 750 840 900 790 990 920 940 780 910] 15687 15688 980 15689 [850 920 960 830 840 980] 15696 [960 990 960] 15699 15700 990 15701 [880 710 760 750 620 720 820 730 680 790 750] 15712 15713 760 15714 [700 680 750 680 740] 15725 15726 676 15727 [826 816 871 901 707 601] 15733 15736 597 15737 [660 340 514 539 538 541] 15743 15744 584 15745 [532 340 276 433 340 433 377 468 340 468 595 660 514] 15758 15759 609 15760 [535 708] 15762 15763 707 15764 [727 595 695 541 367 532 264] 15771 15772 472 15773 [543 619 607] 15776 15777 601 15778 [589 366 597 369 340 609 689 703 405 538 695 472 602 601 276 433 597 901 548 358 371 606 366 369 607 601 390 433 468 366 268 416 601 299 518 602 901 551 601 435 418 601 591 535 612 607 274 602 559 265 593 543 479 587 532 901 568 515 591 800 601 416 422 521 567 366 601 0] 15846 15847 340 15848 [336 567 340] 15851 15855 524 15856 15857 478 15858 15866 0 15867 [446] 15868 15879 0 15880 [372 695 645 547 511 265 604 750 712 551 575 557 594 428 401 214 578 325 371 189 574 588 846] 15903 15904 676 15905 15907 677 15908 [401 604 579 503 724 373 803 846 870 683 578] 15919 15922 574 15923 [620 483] 15925 15926 509 15927 [518] 15928 15929 556 15930 [566 264] 15932 15933 411 15934 [346] 15935 15936 449 15937 [580 620 483] 15940 15941 586 15942 [516 690] 15944 15945 683 15946 [709 580 669 518 359 566 258] 15953 15954 449 15955 [499 579 570] 15958 15959 578 15960 [565 359 574 346 586 671 679 395 509 669 449 567 578 264 411 574] 20317 [344 422 397 383] 20322 [715 709 680 514 707 541 540 524 325 601 369 609 535] 20335 20337 689 20338 20339 276 20340 [600] 20341 20342 514 20343 [707 595] 20345 20349 695 20350 [921 591 472 543] 20354 20356 602 20357 [265 524 268 264 601 367 597 800 518 927 623 669] 20369 20370 676 20371 [594 398 422 390 396 964 699 679 663 486 682 518 517 509 319 578 346 586 516] 20390 20392 671 20393 20394 270 20395 [574] 20396 20397 483 20398 [683 580] 20400 20404 669 20405 [901 566 449 499] 20409 20411 567 20412 [258 508] 20414 20415 258 20416 [578 359 574 770 489 927 644 669] 20424 20425 676 20426 [594]");
        return info;
    }

    private Map<String, String> getJASerifInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "KozMinPr6N-Regular");
        info.put("Ordering", "Japan1");
        info.put("Supplement", "6");
        info.put("DW", "1000");
        info.put("Encoding", "UniJIS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "742");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-437,-340,1147,1317");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [278 299 353] 4 5 614 6 [721 735 216] 9 10 323 11 [449 529 219 306 219 453] 17 26 614 27 28 219 29 31 529 32 [486 744 646 604 617 681 567 537 647 738 320 433 637 566 904 710 716 605 716 623 517 601 690 668 990 681 634 578 316 614 316 529 500 387 509 566 478 565 503 337 549 580 275 266 544 276 854 579 550 578 566 410 444 340 575 512 760 503 529 453 326 380 326 387 216 453 216 380 529 299] 102 103 614 104 [265 614 475 614 353 451] 110 111 291 112 [588 589 500] 115 116 476 117 [219 494 452 216] 121 122 353 123 [451] 125 [1075 486] 127 137 387 139 [880 448 566 716 903 460 805 275 276 550 886 582 529 738 529 738 357 529] 157 158 406 159 [575 406] 161 163 934 164 169 646 170 [617] 171 174 567 175 178 320 179 [681 710] 181 185 716 186 [529] 187 190 690 191 [634 605] 193 198 509 199 [478] 200 203 503 204 207 275 208 [550 579] 210 214 550 215 [529] 216 219 575 220 [529 578 529 517 634 578 445 444 842 453 614] 231 632 500 8718 8719 500 9354 [614 684 216 353 648 899 903 509 275 575 503 550 646 320 690 567 716] 9371 9376 934 9377 9384 426 9385 9387 425 9388 [439] 9389 9393 426 9394 [646] 9395 9397 567 9398 9400 320 9401 9402 716 9403 9405 690 9406 [509] 9407 9409 503 9410 9412 275 9413 9414 550 9415 9417 575 9418 9420 513 9421 9422 805 9423 9425 478 9426 9428 503 9429 9431 735 9432 9434 426 9435 [266 578 550] 9438 9440 512 9441 [640 594 284 257 281 288] 9447 9448 546 9449 [703 705 160 312 305 389 545 200 309 200 438] 9460 9469 546 9470 9471 200 9472 9474 545 9475 [471 744 607 572 563 650 550 516 622 696 312 314 603 524 848 665 644 561 645 583 491 566 643 581 872 616 537 516 312 546 312 472 464 400 548 530 447 558 460 301 486 564 283 258 509 265 834 578 521 554 551 390 410 335 565 476 717 525 464 456 312 339 312 400 179 422 177 339 545 281] 9545 9546 546 9547 [248 546 491 570 310 440 278 279 556 563 586] 9558 9559 403 9560 [207 500 440 170 307 310 440 786 1009 471 367] 9571 9576 400 9577 [364 400 365 400 1012 849 394 544 644 889 377 744 283 285 521 808 545 504 703 545 703 324 504] 9600 9601 397 9602 [557 397] 9604 9606 859 9607 9612 607 9613 [562] 9614 9617 550 9618 9621 312 9622 [662 665] 9624 9628 644 9629 [497] 9630 9633 643 9634 [537 576] 9636 9641 548 9642 [446] 9643 9646 460 9647 9650 283 9651 [522 578] 9653 9657 521 9658 [545] 9659 9662 565 9663 [464 540 464 491 537 516 418 410 842 456 546 563 627 196 289 560 828 835 548 283 565 460 521 607 312 643 550 644] 9691 9696 859 9697 9713 397 9714 [607] 9715 9717 550 9718 9720 312 9721 9722 644 9723 9725 643 9726 [548] 9727 9729 460 9730 9732 283 9733 9734 521 9735 9737 565 9738 9757 250 9758 9778 333 12063 12087 500 15449 [670 810 640 760] 15455 [980] 15456 15458 529 15459 [619 529 891] 15464 15467 529 15468 15469 534 15470 15471 566 15472 [530] 15473 15476 529 15477 [581] 15478 15479 529 15480 15481 533 15482 [738 853 676 533] 15486 15488 882 15489 15490 716 15491 [600 529 589 688] 15495 15496 529 15497 15498 559 15499 [594 619 522 529 468 721] 15505 15507 529 15508 [525] 15509 15510 529 15511 [661 529 660 564 684 500 790 940 870] 15521 [630] 15522 15523 740 15524 [900 840 980] 15529 [900 940 710 870 970] 15535 [820 930 840 940] 15539 15540 850 15541 15542 960 15545 [960 980 940 970 910 950 870] 15552 15553 980 15554 [910 930 820 850 980 950] 15562 [970] 15563 15565 980 15575 [980] 15576 15577 990 15578 [880] 15581 [960 850 860] 15585 [840 950 740 870 830 760 890 990 900 870 990 970 980 950 960] 15601 [850 830 950 930 810 980 910 780 890 760 880 790 870 830 980 830] 15617 15618 900 15619 [950 940 950 820 910 930 960 880 930 960 980 920 940 920 950 970 980 890 930 860 930 960 990 820 920 960 930 970 760 780 920 970 830 950 830] 15654 15656 990 15657 [840] 15658 15659 890 15660 [900] 15661 15662 940 15663 15665 980 15666 15668 970 15669 [960 800 950 820 960 810 950 810 990 730 850 880 760 990 910 920 770 870 970 980 840 920 950 810 800 940 950 900 960 910] 15699 15700 960 15701 [750 740 860 850 640 690 900 750 740 840 770 800 790 730 640 860 760 790] 15723 [770 780] 15725 15726 529 15727 [934 841 904 854 710 579] 15733 15736 575 15737 [646 387 566] 15740 15741 517 15742 [601] 15743 15744 578 15745 [509 387 313 444 387 444 340 453 387 453 623 646 566] 15758 15759 617 15760 [567 681] 15762 15763 710 15764 [716 623 690 601 410 509 276] 15771 15772 478 15773 [503 605 565] 15776 15777 579 15778 [550 410 575 340 387 617 647 738 433 517 690 478 549 580 266 444 575 846 524 396 341 565 408 291 560 574 410 444 453 383 262 266 579 261 519 563 854 555 580] 15817 15818 370 15819 [580 716 405 566 565 280 566 650 275 575 503 426 505 509 854 551 546 534 760 576] 15839 15840 370 15841 [494 506 389 578 0] 15846 15847 234 15848 [282 523 387] 15851 15857 405 15858 15866 0 15867 [373] 15868 15879 0 15880 [468] 15881 15882 624 15883 [541 484 367 580 738 635 555 505 546 598 500 447 290 474 310 331 220 466 632 976] 15903 15904 529 15905 15907 882 15908 [446 526 544 431 627 500 859 848 834 665 578] 15919 15922 565 15923 [607 524] 15925 15926 491 15927 [566] 15928 15929 516 15930 [548 265] 15932 15933 410 15934 [335] 15935 15936 456 15937 [583 607 524] 15940 15941 563 15942 [550 650] 15944 15945 665 15946 [644 583 643 566 390 548 265] 15953 15954 447 15955 [460] 15956 15957 558 15958 15959 578 15960 [521 390 565 335 563 622 696 314 491 643 447 486 564 258 410 565] 20317 20318 387 20319 [460 448 814 681 738 748 566 694 601 523 544 310 579 340 617 567] 20335 20337 647 20338 20339 320 20340 [637] 20341 20342 566 20343 [710 623] 20345 20349 690 20350 [990 634 478 503] 20354 20356 549 20357 [275 544] 20359 20360 276 20361 [579 410 575 760 529 905 670 730] 20369 20371 588 20372 20373 400 20374 [377 394 739 662 696 626 524 632 566 541 509 281 578 335 563 550] 20390 20392 622 20393 20394 312 20395 [603] 20396 20397 524 20398 [665 583] 20400 20404 643 20405 [872 537 447 460] 20409 20411 486 20412 [283 509] 20414 20415 265 20416 [578 390 565 717 464 1010 602 744] 20424 20425 545 20426 [643]");
        return info;
    }

    private Map<String, String> getCNSansInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeHeitiStd-Regular");
        info.put("Ordering", "GB1");
        info.put("Supplement", "5");
        info.put("DW", "1000");
        info.put("Encoding", "UniGB-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "766");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-163,-283,1087,967");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [224 266 392 551 562 883 677 241] 9 10 322 11 [470 677 247 343 245 370] 17 26 562 27 [245 247] 29 31 677 32 [447 808 661 602 610 708 535 528 689 703 275 404 602 514 871 708 727 585 727 595 539 541 696 619 922 612 591 584 322 362 322 677 568 241 532 612 475 608 543 332 603 601 265 276 524 264 901 601 590 612 607 367 433 369 597 527 800 511 518 468 321 273 321 677] 814 939 500 7712 [597 769 683 1136 500] 22353 22354 562 22355 22357 500 29064 30283 550");
        return info;
    }

    private Map<String, String> getCNSerifInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeSongStd-Light");
        info.put("Ordering", "GB1");
        info.put("Supplement", "5");
        info.put("DW", "1000");
        info.put("Encoding", "UniGB-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "626");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-134,-254,1001,905");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [207 270 342 467 462 797 710 239] 9 10 374 11 [423 605 238 375 238 334] 17 26 462 27 28 238 29 31 605 32 [344 748 684 560 695 739 563 511 729 793 318 312 666 526 896 758 772 544 772 628 465 607 753 711 972 647 620 607 374 333 374 606 500 239 417 503 427 529 415 264 444 518 241 230 495 228 793 527] 80 81 524 82 [504 338 336 277 517 450 652 466 452 407 370 258 370 605] 814 939 500 7712 [517 684 723] 7716 [500] 22353 22354 462 22355 22357 500 29064 30283 550");
        return info;
    }

    private Map<String, String> getTWSansInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeFanHeitiStd-bold");
        info.put("Ordering", "CNS1");
        info.put("Supplement", "5");
        info.put("DW", "1000");
        info.put("Encoding", "UniCNS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "766");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-163,-283,1087,967");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [224 266 392 551 562 883 677 213] 9 10 322 11 [470 677 247 343 245 370] 17 26 562 27 [245 247] 29 31 677 32 [447 808 661 602 610 708 535 528 689 703 275 404 602 514 871 708 727 585 727 595 539 541 696 619 922 612 591 584 322 362 322 677 568 340 532 612 475 608 543 332 603 601 265 276 524 264 901 601 590 612 607 367 433 369 597 527 800 511 518 468 321 273 321 677 769 683 1136] 13648 13742 500 17601 [562] 17603 [500]");
        return info;
    }


    private Map<String, String> getTWSerifInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeMingStd-Light");
        info.put("Ordering", "CNS1");
        info.put("Supplement", "5");
        info.put("DW", "1000");
        info.put("Encoding", "UniCNS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "731");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-38,-121,1002,918");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [251 347 405 739 504 758 825 281 293 294 494 620 251 373 252 309] 17 20 503 21 [504 503 502] 24 25 503 26 [504] 27 28 251 29 31 621 32 [405 1042 749 673] 36 37 679 38 [685 671 738 736 333 494 729 696 901 720 750 674 746 672 627 769 707 777 887 709 716 616 279 309 277 352 575 294 500 511 502 549 494 356 516 550] 74 75 321 76 [510 317 738 533 535 545 533 376 443 361 529 526 742 534 576 439 447 262 446 472] 13648 13742 500 17601 [639] 17603 [500]");
        return info;
    }

    private Map<String, String> getKOSansInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeGothicStd-Bold");
        info.put("Ordering", "Korea1");
        info.put("Supplement", "2");
        info.put("DW", "1000");
        info.put("Encoding", "UniKS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "769");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-165,-285,1092,972");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [216 283 414 567 572 901 699 218] 9 10 331 11 [483 680 272 342 271 361] 17 26 572 27 [271 272] 29 31 680 32 [456 813 663 625 599 706 544 537 688 708 298 414 623 522 874 711 720 600 720 614 548 552 698 638 923 612 599 579 331 345 331 680 571 342 548 615 469 613 551 354 606 608 281 294 550 281 902 608 593 615 612 386 443 385 604 540 803 529 532 478 331 288 331 680 923 570 773 690 1142] 8094 8190 500");
        return info;
    }

    private Map<String, String> getKOSerifInfo()
    {
        Map<String, String> info = new HashMap<String, String>();
        info.put("BaseFont", "AdobeMyungjoStd-Medium");
        info.put("Ordering", "Korea1");
        info.put("Supplement", "2");
        info.put("DW", "1000");
        info.put("Encoding", "UniKS-UTF16-H");
        info.put("Ascent", "880");
        info.put("CapHeight", "719");
        info.put("Descent", "-120");
        info.put("Flags", "4");
        info.put("FontBBox", "-28,-148,1001,883");
        info.put("ItalicAngle", "0");
        info.put("StemV", "80");
        info.put("W", "1 [333] 2 3 416 4 [833 625 916 833 250] 9 11 500 12 [833 291 833 291 375] 17 26 625 27 28 333 29 30 833 31 [916 500] 34 [791] 35 36 708 37 [750 708 666 750 791 375 500 791 666 916 791 750 666 750 708 666] 53 54 791 55 [750] 57 58 708 59 [666 500 375] 62 64 500 65 [333 541 583 541] 69 70 583 71 [375] 72 73 583 74 [291 333 583 291 875] 79 82 583 83 [458 541 375] 86 87 583 88 [833] 89 90 625 91 [500] 92 94 583 95 [750] 97 [500] 8094 8190 500");
        return info;
    }

    private PdfArray getWArray(String w)
    {
        StringTokenizer st = new StringTokenizer(w);
        boolean inArray = false;
        PdfArray warray = new PdfArray();
        PdfArray array  = null;
 
        while (st.hasMoreTokens())
        {
            String token = st.nextToken();
            if (token.charAt(0) == '[')
            {
                array = new PdfArray();
                token = token.substring(1);
                inArray = true;
            }
            if (token.endsWith("]"))
            {
                token = token.substring(0, (token.length() - 1));
                array.add(new PdfInteger(Integer.parseInt(token)));
                warray.add(array);
                inArray = false;
            }
            else if (inArray)
            {
                array.add(new PdfInteger(Integer.parseInt(token)));
            }
            else
            {
                warray.add(new PdfInteger(Integer.parseInt(token)));
            }
         }

         extractWidths(warray);
         setUniToCid();

         return warray;
    }

    private void extractWidths(PdfArray widths) 
    {
        widthCache = new HashMap<Integer, Integer>();
        if( widths != null )
        {
            int size = widths.size();
            int counter = 0;
            while (counter < size) 
            {
                PdfInteger firstCode = (PdfInteger)widths.get( counter++ );
                PdfDirectObject next = widths.get( counter++ );
                if( next instanceof PdfArray )
                {
                    PdfArray array = (PdfArray)next;
                    int startRange = firstCode.getIntValue();
                    int arraySize = array.size();
                    for (int i=0; i<arraySize; i++) 
                    {
                        PdfInteger width = (PdfInteger)array.get(i);
                        widthCache.put(startRange+i, width.getValue());
                    }
                }
                else
                {
                    PdfInteger secondCode = (PdfInteger)next;
                    PdfInteger rangeWidth = (PdfInteger)widths.get( counter++ );
                    int startRange = firstCode.getIntValue();
                    int endRange = secondCode.getIntValue();
                    Integer width = rangeWidth.getValue();
                    for (int i=startRange; i<=endRange; i++) {
                        widthCache.put(i, width);
                    }
                }
            }
        }
    }


    private int[] toCodePointArray(String text)
    {
        int len = text.length(); 
        int[] cpArray = new int[text.codePointCount(0, len)];
        int j = 0;

        for (int i = 0, cp; i < len; i += Character.charCount(cp)) {
            cp = text.codePointAt(i);
            cpArray[j++] = cp;
        }
        return cpArray;
    }

    private int getUnicode(String str)
    {
        int len = str.length();
        byte[] code = new byte[len/2];
        int j = 0;

        for (int i=0; i<len; i+=2)
        {
            code[j++] = Integer.valueOf(str.substring(i, i+2), 16).byteValue();
        }

        return new String(code, Charset.forName("UTF-16BE")).codePointAt(0);
    }

    private void setUniToCid()
    {
        try 
        {
            if (uni2cid == null)
            {
                uni2cid = new HashMap<Integer, Integer>();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(CJKFont.class.getResourceAsStream("/fonts/cmap/"+info.get("Encoding"))));
                String line;
                while ((line = reader.readLine()) != null)
                {
                    if (line.endsWith(BEGIN_CID_CHAR))
                    {
                        inCidChar = true;
                    }
                    else if (line.equals(END_CID_CHAR))
                    {
                        inCidChar = false;
                    }
                    else if (line.endsWith(BEGIN_CID_RANGE))
                    {
                        inCidRange = true;
                    }
                    else if (line.equals(END_CID_RANGE))
                    {
                        inCidRange = false;
                    }
                    else if (inCidChar)
                    {
                        Matcher matcher = CID_PATTERN.matcher(line);
                        if (matcher.matches())
                        {
                            int code = getUnicode(matcher.group(1));
                            Integer cid = Integer.valueOf(matcher.group(2));
                            uni2cid.put(code, cid);
                        }
                    }
                    else if (inCidRange)
                    {
                        Matcher matcher = CID_RANGE_PATTERN.matcher(line);
                        if (matcher.matches())
                        {
                            int start = getUnicode(matcher.group(1));
                            int end   = getUnicode(matcher.group(2));
                            Integer cid = Integer.valueOf(matcher.group(3));
                            for (int i=0; i<(end-start+1); i++)
                            {
                                uni2cid.put(start+i, cid+i);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
