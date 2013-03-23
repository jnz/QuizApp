package javaquiz;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.util.EventListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Random;

/**
 * QuizView is responsible for displaying the game in a Swing JFrame window and
 * sending back user interaction to the QuizController with the
 * EventListener/Observer pattern.  The QuizController has to call init(), and
 * register the callback interface with the addViewListener() method.
 */
public class QuizViewSwing implements QuizView
{
    /** The display frame, this the main game window. */
    private QuizFrame m_frame = null;
    /** List of object that are interested in this view (Observer pattern).
     *  If the user is doing something, this class notifies the listeners. */
    private EventListenerList m_viewlistener;
    /** The current language object. */
    private QuizLanguage m_language = null;
    /** Last known game state to compare to the current game state. */
    private QuizModel.QuizState m_lastState = QuizModel.QuizState.NULL;

    /** Default constructor. Sets the window style to the
     * native look and feel. */
    public QuizViewSwing()
    {
        // we create a default window
        setNativeLookAndFeel();
        m_viewlistener = new EventListenerList();
    }

    /** Update the view to display the current game state from the model.
     * @param q The current quiz model (=game state).
     * @param time Time is the absolute time in [ms].
     * @param dt Time since the state has changed in [s] (0.0f if the state has just changed). This is not the time since the last frame!
     * @param forceRedraw Redraw the screen in any case. Mainly used by setLanguage. */
    public void update(QuizModel q, long time, float dt, boolean forceRedraw)
    {
        // let's see if the state has changed or if forceRedraw has been set.
        // We need to redraw the screen for most state changes.
        boolean stateChanged = (m_lastState != q.getState()) || forceRedraw;

        // update the glass pane effect (unless we are in the intro)
        if(q.getState() != QuizModel.QuizState.BEGIN)
        {
            QuizGlassPane glasspane = (QuizGlassPane)m_frame.getGlassPane();
            glasspane.update(dt);
            glasspane.repaint();
        }

        switch(q.getState())
        {
            case BEGIN: /* intro screen */
                if ( stateChanged ) // state is new, so we update
                {
                    m_frame.m_background.setBackground(Color.BLACK);
                    m_frame.displayIntro();
                }
                if(dt <= 2.0f) /* fade in of the intro screen for 2 seconds. */
                {
                    m_frame.m_background.setAlpha(Math.min(dt, 2.0f)/2.0f);
                    m_frame.repaint();
                }
                break;
            case ASKING:
                if ( stateChanged )
                {
                    m_frame.displayQuestion(q, q.getLevel()-1);
                }
                break;
            case WRONG_ANSWER:
                if ( stateChanged )
                {
                    m_frame.displayWrongAnswer();
                }
                else if(dt <= 0.5f) // animate the correct button for 0.5 secs.
                {
                    // animate the green glow button to build up tension.
                    QuizButton btn = m_frame.getSelectedButton();
                    if(btn != null)
                    {
                        // alpha = f(dt) (for the 0.5 seconds)
                        btn.setAlpha(0.5f + 0.5f*(float)Math.cos(dt/0.5f*2*Math.PI));
                        // at 0.25 seconds activate the red glow
                        if(dt > 0.25 && btn.getWrong() == false)
                        {
                            btn.setWrong(true);
                        }
                        btn.repaint();
                    }
                }
                break;
            case RIGHT_ANSWER:
                if ( stateChanged )
                {
                    m_frame.displayRightAnswer();
                }
                else if(dt <= 0.5f) // animate the correct button for 0.5 secs.
                {
                    // animate the green glow button to build up tension.
                    QuizButton btn = m_frame.getSelectedButton();
                    // alpha = f(dt) (for the 0.5 seconds)
                    btn.setAlpha(0.5f + 0.5f*(float)Math.cos(dt/0.5f*2*Math.PI));
                    // at 0.25 seconds activate the green glow but only do this once
                    if(dt > 0.25 && btn.getRight() == false)
                    {
                        btn.setRight(true);
                    }
                    btn.repaint();
                }
                // animate the orange bar, moving up, depending on dt
                m_frame.setScoreboardAnimation(Math.min(dt, 1.0f));
                break;
            case GAMEOVER:
                if(stateChanged)
                {
                    // game over for the user.
                    m_frame.displayGameOver(q.getCurrentQuestion(), q.getFallbackLevel());
                }
                else
                {
                    // for a dramatic effect, the screen is at first black
                    // and then fades in to the normal brightness leve.
                    // this effect takes four seconds.
                    if(dt <= 4.0f)
                    {
                        m_frame.m_background.setAlpha(Math.min(dt, 4.0f)/4.0f);
                        m_frame.repaint();
                    }
                }
                break;
            case GAMEWON:
                if(stateChanged)
                {
                    // final state: the user is a millionaire.
                    m_frame.displayGameWon(q.getCurrentQuestion(), q.getLevel());
                }
                else
                {
                    // linear white "fade in" effect
                    if(dt <= 4.0f)
                    {
                        m_frame.m_background.setAlpha(Math.min(dt, 4.0f)/4.0f);
                        m_frame.repaint();
                    }
                }
                break;
            case JOKER_AUDIENCE:
                // not implemented
                assert false;
                break;
            case JOKER_5050:
                int[] eliminate = q.getEliminatedQuestions();

                if(stateChanged) // prevent the user from clicking the buttons (lock them).
                {
                    for(int i=0;i<eliminate.length;i++)
                    {
                        m_frame.getButton(eliminate[i]).setLocked(true);
                    }
                }
                // animate the eliminated questions:
                // animate them for 1.5 seconds with an alpha blending effect.
                // alpha = f(t)
                // alpha(0) = 1, alpha(0.75) = 0, alpha(1.5) = 1
                float alpha = 0.5f + 0.5f*(float)Math.cos(2.0*Math.PI*(1.5 - dt)/1.5);
                for(int i=0;i<eliminate.length;i++)
                {
                    m_frame.getButton(eliminate[i]).setAlpha(alpha);
                    m_frame.getButton(eliminate[i]).repaint();

                    // When alpha is zero (at dt = 0.75), we set the text to ""
                    if(dt > 0.75 && m_frame.getButton(eliminate[i]).getText().length() > 0)
                    {
                        m_frame.getButton(eliminate[i]).setText("");
                    }
                }

                break;
            default:
                // this should not happen
                Quiz.Print("Unknown state.");
                assert false;
        }

        m_lastState = q.getState();
    }

    /** Create a new view window. */
    public void init()
    {
        m_lastState = QuizModel.QuizState.NULL;
        makeNewWindow();
    }

    /** Gets the current language object.
     * @return Returns the current active language object. */
    public QuizLanguage getLanguage()
    {
        return m_language;
    }

    /** Sets the language. All UI elements are updated.
     * @param language The language object. */
    public void setLanguage(QuizLanguage language, QuizModel q)
    {
        m_language = language;
        m_frame.setLanguage(language, q);
    }

    /** Add event listener for this view.
     * Events are used to notify the game logic when the
     * user is interacting with the UI.
     * @param l Action listener (normally the QuizController). */
    public void addViewListener(ActionListener l)
    {
        m_viewlistener.add(ActionListener.class, l);
    }

    /** Remove event listener for this view.
     *  @param l Action listener to remove. */
    public void removeViewListener(ActionListener l)
    {
        m_viewlistener.remove(ActionListener.class, l);
    }

    /** Notify all listeners (i.e. QuizController),
     *  that the user has clicked a button.
     *  @param button Index of pressed button. */
    private void fireEventButtonSelect(int button)
    {
        fireEvent("button" + button);
    }

    /** Notify all listeners, that the user
     *  wants to restart the game. */
    private void fireEventRestartGame()
    {
        fireEvent("restart");
    }

    /** Notify all listeners, that the user
     *  wants to toggle the language. */
    private void fireEventToggleLanguage()
    {
        fireEvent("togglelanguage");
    }

    /** Notofy all listeners (i. e. normally the QuizController)
     * that the user has done something.
     * @param cmd Command string describing the event. */
    private void fireEvent(String cmd)
    {
        EventListener listenerList[] = m_viewlistener.getListeners(ActionListener.class);
        for (int i = 0; i < listenerList.length; i++)
            ((ActionListener)listenerList[i]).actionPerformed(new ActionEvent(this, 0, cmd));
    }

    /** Use the operating system application style. */
    private static void setNativeLookAndFeel()
    {
        // Normally this should work. In the worst case, the user
        // gets the default swing interface (not critical).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e) {
            Quiz.Print("Failed to switch to native look and feel.\n");
        }
    }

    /** Create our main game window and all the components in it. */
    protected void makeNewWindow()
    {
        m_frame = new QuizFrame(this);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        m_frame.setLocation(0, 0);
        m_frame.setSize(screenSize.width, screenSize.height);

        m_frame.setVisible(true);
    }

    /**
     *  This subclass of JPanel displays a transparent (single value alpha-blended)
     *  background image without alpha channel evaluation.
     *  While QuizButton is using the png alpha channel, this
     *  panel is using one alpha value for blending.
     */
    class TransparentPanel extends JPanel
    {
        /** Image file for this panel. */
        private BufferedImage m_img;
        /** Alpha factor for this component. */
        private float m_alpha;
        /** If false, the image is scaled to the component dimension. */
        private boolean m_keepAspectRatio = false;

        /** Creates a panel with a background image and an alpha value.
         * @param img Precached image for this panel.
         * @param alpha Alpha blending factor. 0.0f = invisible, 1.0f = opaque. */
        public TransparentPanel(BufferedImage img, float alpha)
        {
            setBackground(img);
            m_alpha = alpha;
            setOpaque(false);
        }

        /** Set the alpha blending value.
         * @param alpha Alpha blending factor. 1.0f = opaque, 0.0f = invisible. */
        public void setAlpha(float alpha)
        {
            m_alpha = alpha;
        }

        /** Set the "keep aspect ratio property".
         * @param keepAspectRatio If false, the image is scaled to the component dimension. */
        public void setKeepAspectRatio(boolean keepAspectRatio)
        {
            m_keepAspectRatio = keepAspectRatio;
        }

        /** Set a new background image.
         * @param img Precached image. */
        public void setBackground(BufferedImage img)
        {
            m_img = img;
        }

        /** Custom draw function for the background image.
         * @param g Graphics object with Graphics2D support. */
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            Composite oldComp = g2d.getComposite();

            // single alpha value blending effect.
            Composite alphaComp = AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, m_alpha);

            // bilinear filtering for the stretched bitmap
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int height;
            int width;
            int y = 0;
            int x = 0;

            // if keepaspectratio is set to true,
            // we need to do some calculations to center
            // the bitmap.
            if(m_keepAspectRatio)
            {
                // keep the image aspect ratio and center it.
                height = getHeight();
                float ratio = m_img.getWidth()*1.0f/m_img.getHeight(); // aspect ratio
                width = (int)(height * ratio);
                y = 0;
                x = (getWidth() - width)/2;
            }
            else
            {
                x = 0;
                y = 0;
                width = getWidth();
                height = getHeight();
            }

            // set the alpha blending effect
            g2d.setComposite(alphaComp);
            // draw the background image
            g2d.drawImage(m_img, x, y, width, height, this);
            // restore the old settings
            g2d.setComposite(oldComp);
        }
    }

    /**
     * QuizScoreboard displays the current score.
     */
    class QuizScoreboard extends JPanel
    {
        /** The current level in the game. */
        private int m_level = 0;
        /** The current animation state from 0 to 1. */
        private float m_animation = 0.0f;

        /** Create a scoreboard component. */
        QuizScoreboard()
        {
            setOpaque(false);

            // hardcode some dimensions
            final int minwidth = 280;
            final int minheight = 450;
            final int maxwidth = 415;
            final int maxheight = 560;
            setMinimumSize(new Dimension(minwidth, minheight));
            setMaximumSize(new Dimension(maxwidth, maxheight));
        }

        /** Implement the preferred size method.
         * @return The preferred size (Dimension object). */
        public Dimension getPreferredSize()
        {
            int newwidth = getHeight()*3/4;
            newwidth = Math.min(newwidth, 415);
            newwidth = Math.max(newwidth, 280);

            //return new Dimension(newwidth, 480);
            return new Dimension(400, 400);
        }

        /** Set the current level.
         * @param level New level. */
        public void setLevel(int level)
        {
            m_level = level;
        }

        /** Animate the scoreboard change.
         * @param f A factor between 0 and 1 for the animation. */
        public void setAnimation(float f)
        {
            assert f >= 0.0f && f <= 1.0f;
            m_animation = f;
        }

        /** What currency string should be displayed. */
        public static final String m_currencyString = "$ ";
        //public static final String m_currencyString = " \u20AC"; // Euros

        /** Custom draw function for the score board.
         * @param g Graphics context (Graphics2D capable). */
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D)g;

            final int width = getWidth();
            final int height = getHeight();
            final int[] scoretable = QuizModel.getScoretable(); // game levels
            final int itemheight = height/scoretable.length; // height of one item

            // font height
            final int shadowoffset = 1; // shadow offset in pixel
            final int strpadding = 2; // pixel amount of space
            final int leftoffset = 12; // pixel offset from left border
            final int strheight = Math.max(2, itemheight - strpadding*2);
            Font f = new Font("Arial", Font.PLAIN, strheight);
            g2d.setFont(f);

            // draw the rectangle with alpha blending
            Composite oldComp = g2d.getComposite();
            Composite alphaComp =
                AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f);
            // font anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2d.setColor(Color.BLUE);
            g2d.setComposite(alphaComp);
            // nice color gradient
            g2d.setPaint(new GradientPaint(0, 0, Color.BLUE, width, 0, Color.WHITE));
            g2d.fillRect(0, 0, width, height);

            // draw each line in the quiz scoreboard
            for(int i=0; i<scoretable.length; i++)
            {
                if(scoretable.length - i - 1 == m_level) // draw the current level orange
                {
                    // highlight current level
                    g.setColor(Color.ORANGE);
                    // transparent while animated
                    if(m_animation == 0.0f)
                        g2d.setComposite(oldComp); // no animation, opaque
                    else
                        g2d.setComposite(alphaComp);

                    // animate the orange highlight level
                    int ypos = i*itemheight;
                    ypos -= (int)(itemheight*m_animation + 0.5f);
                    g.fillRect(0, ypos, width, itemheight);
                }
                else
                {
                    // blue background for all other levels
                    g.setColor(Color.BLUE);
                    g2d.setComposite(alphaComp);
                }

                // create string for current level
                NumberFormat nf = NumberFormat.getInstance();
                String number = m_currencyString + " " + nf.format(scoretable[scoretable.length - i - 1]);

                // draw a black text shadow for the current level
                if(scoretable.length - i - 1 == m_level)
                {
                    g.setColor(Color.BLACK);
                    g2d.drawString(number,
                                   strpadding + leftoffset + shadowoffset,
                                   itemheight + i*itemheight - 3*strpadding + shadowoffset);
                }

                // draw the text
                if(scoretable.length - i - 1 <= m_level)
                    g.setColor(Color.WHITE); // reached levels in white
                else
                    g.setColor(Color.BLACK); // unreached levels in gray

                g2d.drawString(number,
                               strpadding + leftoffset,
                               itemheight + i*itemheight - 3*strpadding);
            }
        }
    }

    /** QuizButton is our custom button class for the answers.
     * */
    static class QuizButton extends JPanel implements MouseListener
    {
        /** image file for the button. */
        private BufferedImage m_imgNormal;
        /** image file, when the mouse is over the button. */
        private BufferedImage m_imgMouseHover;
        /** image file, when an answer is selected. */
        private BufferedImage m_imgSelected;
        /** image file, when the answer is wrong. */
        private BufferedImage m_imgWrong;
        /** image file, when the answer is right. */
        private BufferedImage m_imgRight;
        /** Don't react to user interaction if locked is true (mouse over and click) */
        private boolean m_locked = false;
        /** Draw the green right answer glow? */
        private boolean m_right = false;
        /** Draw the red wrong answer glow? */
        private boolean m_wrong = false;
        /** Has the user selected this answer button? */
        private boolean m_selected = false;
        /** Is the mouse currently over this button? */
        private boolean m_hover = false;
        /** The button text (answer text). */
        private String m_text;
        /** Button id */
        private int m_id;
        /** Alpha blending factor. */
        private float m_alpha = 1.0f;

        /** QuizButton constructor.
         * @param id Button id.
         * @param text Answer text.
         * @param imgNormal Loaded image for the normal state.
         * @param imgMouseHover Loaded image for the mouse hover state.
         * @param imgSelected Loaded image for the selected state.
         * @param imgWrong Loaded image for the wrong answer state.
         * @param imgRight Loaded image for the right answer state.
         */
        public QuizButton(int id,
                          String text,
                          BufferedImage imgNormal,
                          BufferedImage imgMouseHover,
                          BufferedImage imgSelected,
                          BufferedImage imgWrong,
                          BufferedImage imgRight)
        {
            m_id = id;
            setOpaque(false);
            m_imgNormal = imgNormal;
            m_imgMouseHover = imgMouseHover;
            m_imgSelected = imgSelected;
            m_imgWrong = imgWrong;
            m_imgRight = imgRight;
            Dimension imgdim = new Dimension(m_imgNormal.getWidth(),
                                             m_imgNormal.getHeight());
            setPreferredSize(imgdim);
            setMinimumSize(imgdim);
            setMaximumSize(imgdim);

            m_text = text;

            addMouseListener(this);
        }

        /** Get the button id.
         * @return Button id (0-3). */
        public int getID()
        {
            return m_id;
        }

        /** Allow/disallow the user to change the button
         * (mouse over, click).
         * @param locked If true, the user can't interact with the button. */
        public void setLocked(boolean locked)
        {
            m_locked = locked;
            repaint(); // update the state
        }

        /** Is the user allowed to click the button?
         * @return true if yes, false otherwise. */
        public boolean getLocked()
        {
            return m_locked;
        }

        /** Set a new button text.
         * @param text New answer text for this button. */
        public void setText(String text)
        {
            m_text = text;
        }

        /** Get the button text.
         * @return The current button text. */
        public String getText()
        {
            return m_text;
        }

        /** Draw a green right button.
         * @param right If true, the button will glow green (depending on your artwork) to indicate a right answer. */
        public void setRight(boolean right)
        {
            m_right = right;
        }

        /** Get the status flag.
         * @return True if this button is glowing green. */
        public boolean getRight()
        {
            return m_right;
        }

        /** Draw a red wrong button.
         * @param right If true, the button will glow red (depending on your artwork) to indicate a wrong answer. */
        public void setWrong(boolean wrong)
        {
            m_wrong = wrong;
        }

        /** Is this the wrong answer?
         * @return Is this currently the button for a wrong answer (red glow). */
        public boolean getWrong()
        {
            return m_wrong;
        }

        /** The user has selected this answer, so we display
         *  the button in another color.
         *  @param selected If true, the button has been selected by the user.
         */
        public void setSelected(boolean selected)
        {
            m_selected = selected;
        }

        /** Is this button selected?
         * @return Was this button selected by the user? */
        public boolean getSelected()
        {
            return m_selected;
        }

        /** Set the alpha blending factor.
         * @param alpha Value between 0.0f and 1.0f. */
        public void setAlpha(float alpha)
        {
            assert alpha >= 0.0f && alpha <= 1.0f;
            if(alpha >= 0.0f && alpha <= 1.0f)
            {
                m_alpha = alpha;
                //repaint();
            }
        }

        /** Custom draw function for the background image.
         * @param g Graphics context. */
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            // decide which image to draw
            BufferedImage img;
            if(m_selected)
                img = m_imgSelected; // user has selected this answer
            else if(m_locked)
                img = m_imgNormal;
            else if(m_hover)
                img = m_imgMouseHover; // mouse is hovering over button
            else
                img = m_imgNormal; // default image

            assert !(m_right && m_wrong); // they should not both be true
            if(m_right)
                img = m_imgRight;
            if(m_wrong)
                img = m_imgWrong;

            int width = getWidth();
            int height = getHeight();

            // draw the current image with the alpha channel + a nice interpolation
            Graphics2D g2d = (Graphics2D)g;

            // alpha blending factor
            if(m_alpha != 1.0f)
            {
                Composite alphaComp = AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, m_alpha);
                g2d.setComposite(alphaComp);
            }

            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(img, 0, 0, width, height, this);

            // enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // draw the text with anti-aliasing
            int strheight = height*4/10;
            Font f = new Font("Arial", Font.BOLD, strheight);
            g2d.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int strwidth = fm.stringWidth(m_text);
            // draw a text background shadow
            g.setColor(Color.BLACK);
            int shadowoffsetX = 1;
            int shadowoffsetY = 1;
            g2d.drawString(m_text, (width - strwidth)/2 + shadowoffsetX,
                          (height + strheight)/2 - 7 + shadowoffsetY);
            // draw the text
            g.setColor(Color.WHITE);
            g2d.drawString(m_text, (width - strwidth)/2, (height + strheight)/2 - 7);
        }

        /** Activate/deactivate glow on hover.
         * @param hover If true, the button is highlighted. */
        public void setHover(boolean hover)
        {
            m_hover = hover;
        }

        /** Mouse hover event.
         * @param e Mouse event. */
        public void mouseEntered(MouseEvent e)
        {
            /*
             *if(m_locked)
             *    return;
             *if(m_selected)
             *    return;
             */
            setHover(true);
            repaint();
        }

        /** Mouse exit event.
         * @param e Mouse event. */
        public void mouseExited(MouseEvent e)
        {
            setHover(false);
            //if(m_selected)
                //return;
            repaint();
        }

        /** Button is clicked event.
         * @param e Mouse event. */
        public void mouseClicked(MouseEvent e)
        {
            if(m_locked)
                return;

            setSelected(true);
            repaint();
        }

        public void mousePressed(MouseEvent e) {}
        public void mouseReleased(MouseEvent e) {}
    }

    /** The internal QuizFrame window. */
    class QuizFrame extends JFrame implements MouseListener
    {
        /** The minimum window size in pixel. */
        private Dimension m_minSize = new Dimension(800, 600);
        /** The owner of this frame. */
        private QuizViewSwing m_framework = null;

        /** The little game menu. */
        private JMenu m_menu;
        /** Menu item to restart the game. */
        private JMenuItem m_item_restart_game;
        /** Menu item to quit the game. */
        private JMenuItem m_item_quit;
        /** Menu item to toggle the language between German and English. */
        private JMenuItem m_item_toggle;

        // Our display resources
        // ---------------------
        /** Image file for the button. */
        private BufferedImage m_imgNormal;
        /** Image file, when the mouse is over. */
        private BufferedImage m_imgMouseHover;
        /** Image file, when the answer is selected. */
        private BufferedImage m_imgSelected;
        /** Image file, when the answer is wrong. */
        private BufferedImage m_imgWrong;
        /** Image file, when the answer is right. */
        private BufferedImage m_imgRight;
        /** Image file for the background. */
        private BufferedImage m_imgBackground;
        /** Image file for the winning background. */
        private BufferedImage m_imgBackgroundWon;
        /** Image file for the loser background. */
        private BufferedImage m_imgBackgroundLost;
        /** Image file for the question background. */
        private BufferedImage m_imgQuestion;
        /** Image file for the intro. */
        private BufferedImage m_imgIntro;
        /** Image file for the icon. */
        private BufferedImage m_imgIcon;
        /** Image file for the audience joker. */
        private BufferedImage m_imgJokerAudi;
        /** Image file for the audience joker (highlighted). */
        private BufferedImage m_imgJokerAudiOn;
        /** Image file for the audience joker (used/disabled). */
        private BufferedImage m_imgJokerAudiOff;
        /** Image file for the fifty:fifty joker. */
        private BufferedImage m_imgJokerFifty;
        /** Image file for the fifty:fifty (highlighted). */
        private BufferedImage m_imgJokerFiftyOn;
        /** Image file for the fifty:fifty (used/disabled). */
        private BufferedImage m_imgJokerFiftyOff;
        /** Image file for the winning screen particle shower. */
        private BufferedImage m_imgParticle;
        /** The questions are displayed here. */
        private JLabel m_questionLabel;
        /** Answer button A. */
        private QuizButton m_btnA;
        /** Answer button B. */
        private QuizButton m_btnB;
        /** Answer button C. */
        private QuizButton m_btnC;
        /** Answer button D. */
        private QuizButton m_btnD;
        /** Scoreboard component. */
        private QuizScoreboard m_scoreboard;
        /** Background image. */
        private TransparentPanel m_background;
        /** Panel for question and answer buttons. */
        private JPanel m_southPanel;
        /** Panen for the joker buttons. */
        private JPanel m_jokerPanel;
        /** Joker button Audience. */
        private QuizButton m_btnJokerAudi;
        /** Joker button 50:50. */
        private QuizButton m_btnJokerFifty;

        /** QuizFrame constructor. This will create directly all
         * the elements in the frame (buttons, labels, etc.).
         * @param view Supervisor QuizViewSwing object. */
        public QuizFrame(QuizViewSwing view)
        {
            super("Quiz");

            m_framework = view;
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            //addWindowListener(m_framework);

            // start the window maximized
            GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
            setMaximizedBounds(e.getMaximumWindowBounds());
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

            setMinimumSize(m_minSize); // we don't want the window to be smaller than this

            m_menu = new JMenu("File"); // placeholder text
            m_menu.setMnemonic(KeyEvent.VK_W);

            // restart
            m_item_restart_game = new JMenuItem("Restart"); // placeholder text
            m_item_restart_game.setMnemonic(KeyEvent.VK_R);
            m_item_restart_game.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEventRestartGame();
                }
            });
            m_menu.add(m_item_restart_game);
            // Toggle language
            m_item_toggle = new JMenuItem("Toggle");
            m_item_toggle.setMnemonic(KeyEvent.VK_L);
            m_item_toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    m_framework.fireEventToggleLanguage();
                }
            });
            m_menu.add(m_item_toggle);
            // quit
            m_item_quit = new JMenuItem("Quit");
            m_item_quit.setMnemonic(KeyEvent.VK_Q);
            m_item_quit.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            m_menu.add(m_item_quit);
            // Toggle language

            JMenuBar menuBar = new JMenuBar();
            menuBar.add(m_menu);
            setJMenuBar(menuBar);

            // add the layout elements (buttons, labels, etc.)
            initGameLayout();
        }

        /**
         * Creates the game layout view and precaches all image resources.
         * Creates all buttons, panels and images.
         */
        private void initGameLayout()
        {
            // Load and precache images
            // So we can e. g. quickly change between the
            // intro background and the normal background.
            try
            {
                m_imgBackground     = ImageIO.read(getClass().getClassLoader().getResourceAsStream("background2.jpg"));
                m_imgBackgroundWon  = ImageIO.read(getClass().getClassLoader().getResourceAsStream("backgroundWon.jpg"));
                m_imgBackgroundLost = ImageIO.read(getClass().getClassLoader().getResourceAsStream("backgroundLost.jpg"));
                m_imgIntro          = ImageIO.read(getClass().getClassLoader().getResourceAsStream("intro.jpg"));
                m_imgQuestion       = ImageIO.read(getClass().getClassLoader().getResourceAsStream("question.png"));
                m_imgNormal         = ImageIO.read(getClass().getClassLoader().getResourceAsStream("button.png"));
                m_imgMouseHover     = ImageIO.read(getClass().getClassLoader().getResourceAsStream("buttonOn.png"));
                m_imgSelected       = ImageIO.read(getClass().getClassLoader().getResourceAsStream("buttonSelected.png"));
                m_imgWrong          = ImageIO.read(getClass().getClassLoader().getResourceAsStream("buttonWrong.png"));
                m_imgRight          = ImageIO.read(getClass().getClassLoader().getResourceAsStream("buttonRight.png"));
                m_imgIcon           = ImageIO.read(getClass().getClassLoader().getResourceAsStream("icon.png"));
                m_imgJokerAudi      = ImageIO.read(getClass().getClassLoader().getResourceAsStream("audiencebutton.png"));
                m_imgJokerAudiOn    = ImageIO.read(getClass().getClassLoader().getResourceAsStream("audiencebuttonOn.png"));
                m_imgJokerAudiOff   = ImageIO.read(getClass().getClassLoader().getResourceAsStream("audiencebuttonOff.png"));
                m_imgJokerFifty     = ImageIO.read(getClass().getClassLoader().getResourceAsStream("5050button.png"));
                m_imgJokerFiftyOn   = ImageIO.read(getClass().getClassLoader().getResourceAsStream("5050buttonOn.png"));
                m_imgJokerFiftyOff  = ImageIO.read(getClass().getClassLoader().getResourceAsStream("5050buttonOff.png"));
                m_imgParticle       = ImageIO.read(getClass().getClassLoader().getResourceAsStream("particle.png"));
            }
            catch(Exception e)
            {
                // If something happens, no matter what, we quit
                // the game, because without the images it surely looks
                // wrong to the user.
                JOptionPane.showMessageDialog(this, "Failed to load images."); // FIXME i18n
                System.exit(0);
            }

            // set the icon
            setIconImage(m_imgIcon);

            /* Layout overview
             *
             *   Joker Buttons
             *  +-----+----+---------------+---------+
             *  |  ?  |  ? |               |         |
             *  +-----+----+  Background   | EAST    | Scoreboard
             *  |     |    |               |         |
             *  +-----+----+-SOUTH---------+---------+ southPanel: parent panel for questionPanel and buttonPanel
             *  +------------------------------------+
             *  |          QUESTION PANEL            | questionPanel
             *  +------------------------------------+
             *  | +---------------+----------------+ |
             *  | |  Button 1     |     Button 2   | | Answer button panel:
             *  | |  Button 3     |     Button 4   | | buttonPanel with a GridLayout
             *  | +---------------+----------------+ |
             *  +------------------------------------+
             */

            // Set the base layout
            setLayout(new BorderLayout());

            // the custom glass pane for the winning effect
            // all states won't show the glass pane, but
            // GAMEWON will enable it (displayGameWon).
            setGlassPane(new QuizGlassPane(m_imgParticle));

            // first we add a background panel.
            // the background panel holds the image. otherwise, we would not
            // need the background panel.
            m_background = new TransparentPanel(m_imgBackground, 1.0f);
            m_background.setOpaque(true);
            m_background.setBackground(Color.BLACK);

            getContentPane().add(m_background, BorderLayout.CENTER);

            // now we place everything in the background panel.
            m_background.setLayout(new BorderLayout(10, 10));
            m_background.setKeepAspectRatio(true); // don't deform the background

            // Add the scoreboard on the right
            m_scoreboard = new QuizScoreboard();
            JPanel scorePanel = new JPanel();

            scorePanel.setOpaque(false);
            scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.X_AXIS));
            scorePanel.add(Box.createHorizontalStrut(15));
            Box box = Box.createHorizontalBox();
            box.add(Box.createGlue());
            box.add(m_scoreboard);
            box.add(Box.createGlue());
            scorePanel.add(box);
            scorePanel.add(Box.createHorizontalStrut(15));

            m_background.add(scorePanel, BorderLayout.EAST);

            // Add the joker buttons on the left
            m_jokerPanel = new JPanel();
            m_jokerPanel.setLayout(new FlowLayout());
            m_jokerPanel.setOpaque(false);

            m_btnJokerAudi = new QuizButton(BUTTON_ID_JOKER_AUDIENCE, "",
                                            m_imgJokerAudi,
                                            m_imgJokerAudiOn,
                                            m_imgJokerAudiOff,
                                            m_imgJokerAudiOff,
                                            m_imgJokerAudiOff);
            m_btnJokerAudi.addMouseListener(this);
            m_btnJokerFifty = new QuizButton(BUTTON_ID_JOKER_FIFTY, "",
                                             m_imgJokerFifty,
                                             m_imgJokerFiftyOn,
                                             m_imgJokerFiftyOff,
                                             m_imgJokerFiftyOff,
                                             m_imgJokerFiftyOff);
            m_btnJokerFifty.addMouseListener(this);
            //m_jokerPanel.add(m_btnJokerAudi);
            m_jokerPanel.add(m_btnJokerFifty);
            m_background.add(m_jokerPanel, BorderLayout.WEST);

            // Everything that is in the south
            m_southPanel = new JPanel();
            m_southPanel.setOpaque(false);
            m_southPanel.setLayout(new GridLayout(2, 1, 0, 10));
            m_background.add(m_southPanel, BorderLayout.SOUTH);

            // Question panel
            m_questionLabel = new JLabel("");
            m_questionLabel.setHorizontalAlignment(JLabel.CENTER);
            m_questionLabel.setForeground(Color.WHITE);
            m_questionLabel.setOpaque(false);
            m_questionLabel.setFont(new Font("Arial", Font.PLAIN, 35));
            setQuestionText("");

            TransparentPanel questionPanel = new TransparentPanel(m_imgQuestion, 0.92f);
            questionPanel.setLayout(new GridLayout(1, 1, 0, 0));
            questionPanel.setMinimumSize(new Dimension(500, 50));
            questionPanel.setPreferredSize(new Dimension(getWidth(), 40));
            questionPanel.setBorder(BorderFactory.createEmptyBorder(5, 60, 5, 60));
            questionPanel.add(m_questionLabel);

            m_southPanel.add(questionPanel);

            // Answer buttons in the south
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(false);
            buttonPanel.setLayout(new GridLayout(2, 2, 0, 0));
            m_southPanel.add(buttonPanel);

            // create the buttons, they all use the same images
            m_btnA = new QuizButton(0, "", m_imgNormal,
                                           m_imgMouseHover,
                                           m_imgSelected,
                                           m_imgWrong,
                                           m_imgRight);
            m_btnA.addMouseListener(this);
            buttonPanel.add(m_btnA);

            m_btnB = new QuizButton(1, "", m_imgNormal,
                                           m_imgMouseHover,
                                           m_imgSelected,
                                           m_imgWrong,
                                           m_imgRight);
            m_btnB.addMouseListener(this);
            buttonPanel.add(m_btnB);

            m_btnC = new QuizButton(2, "", m_imgNormal,
                                           m_imgMouseHover,
                                           m_imgSelected,
                                           m_imgWrong,
                                           m_imgRight);
            m_btnC.addMouseListener(this);
            buttonPanel.add(m_btnC);

            m_btnD = new QuizButton(3, "", m_imgNormal,
                                           m_imgMouseHover,
                                           m_imgSelected,
                                           m_imgWrong,
                                           m_imgRight);
            m_btnD.addMouseListener(this);
            buttonPanel.add(m_btnD);

            // make sure that the initial
            displayIntro();
        }

        /** Sets the current question text. Could also be used for other texts.
         * @param question Text for the Question Label*/
        protected void setQuestionText(String question)
        {
            // by using the html tag, we can have centered multiline text in a
            // JLabel. thanks to stackoverflow for this great idea!
            m_questionLabel.setText("<html><center>" + question + "</center></html>");
        }

        /** Sets the language, based on a existing language object. All UI
         * elements are updated.
         * @param language The language object.
         * @param q The current quiz model object. */
        public void setLanguage(QuizLanguage language, QuizModel q)
        {
            m_menu.setText(language.getString(QuizLanguage.StringID.MENU));
            m_item_restart_game.setText(language.getString(QuizLanguage.StringID.NEWGAME));
            m_item_quit.setText(language.getString(QuizLanguage.StringID.QUIT));
            m_item_toggle.setText(language.getString(QuizLanguage.StringID.TOGGLELANGUAGE));

            Quiz.Print("Switch to language: " + language.getLanguageName());
            Quiz.Print("Current question level: " + (q.getLevel()-1));
            Quiz.Print("Current question: " + q.getCurrentQuestion().getQuestion());
            Quiz.Print("Current state: " + q.getState());
        }

        /** Returns the button that is currently selected.
         *  @return QuizButton object that is selected or null, if no button is selected. */
        public QuizButton getSelectedButton()
        {
            QuizButton btn;
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
            {
                btn = getButton(i);
                if(btn.getSelected())
                    return btn;
            }
            return null; // no button is selected
        }

        /** Get a button based on the index.
         * @param index Button index (0 = A, 1 = B, ...)
         * @return QuizButton object.
         */
        public QuizButton getButton(int index)
        {
            switch(index)
            {
                case BUTTON_ID_ANSWER_A:
                    return m_btnA;
                case BUTTON_ID_ANSWER_B:
                    return m_btnB;
                case BUTTON_ID_ANSWER_C:
                    return m_btnC;
                case BUTTON_ID_ANSWER_D:
                    return m_btnD;
                case BUTTON_ID_JOKER_AUDIENCE:
                    return m_btnJokerAudi;
                case BUTTON_ID_JOKER_FIFTY:
                    return m_btnJokerFifty;
                default:
                    Quiz.Print("Unknown button: " + index);
                    assert false;
                    return null;
            }
        }

        /** Remove all active button effects. */
        private void clearButtonEffects()
        {
            // deselect every button
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
            {
                getButton(i).setSelected(false);
                // clear the right (green) glow effect
                getButton(i).setRight(false);
                // clear the wrong (red) glow effect
                getButton(i).setWrong(false);
                // clear the selected effect
                getButton(i).setSelected(false);
                // clear the hover effect
                //getButton(i).setHover(false); // handled by the mouse enter/leave calls
                // clear the alpha effect
                getButton(i).setAlpha(1.0f);
            }
        }

        /** Display the current question and updates the scoreboard.
         * @param q Current quiz model.
         * @param level Current game level. */
        private void displayQuestion(QuizModel q, int level)
        {
            int i;

            /* spawn the particles at the beginning of the game */
            if(level == 0)
            {
                QuizGlassPane glasspane = (QuizGlassPane)getGlassPane();
                glasspane.spawnParticles(50); // create particles
                glasspane.update(0.0f); // initialize particles
                glasspane.setVisible(true); // display the winning effect
            }

            clearButtonEffects();
            m_btnJokerAudi.setLocked(false);
            m_btnJokerFifty.setLocked(false);

            // update scoreboard
            m_scoreboard.setLevel(level);
            setScoreboardAnimation(0.0f);

            // question related things
            QuizQuestion question = q.getCurrentQuestion();
            if(question != null)
            {
                setQuestionText(question.getQuestion());

                for(i=0;i<BUTTON_ANSWER_COUNT;i++)
                {
                    getButton(i).setLocked(false);
                    getButton(i).setText(question.getAnswer(i));
                }
                // lock the buttons that are affected by the 50:50 joker
                if(q.getJokerFiftyRound() == q.getLevel())
                {
                    int[] eliminated = q.getEliminatedQuestions();
                    for(i=0;i<eliminated.length;i++)
                    {
                        getButton(eliminated[i]).setLocked(true);
                        getButton(eliminated[i]).setText("");
                    }
                }
            }

            // display panels for the question screen
            m_background.setBackground(m_imgBackground);
            m_scoreboard.setVisible(true);
            m_southPanel.setVisible(true);
            m_jokerPanel.setVisible(true);

            repaint();
        }

        /** Animate the scoreboard.
         * @param f A value between 0 and 1 to animate the scoreboard. */
        private void setScoreboardAnimation(float f)
        {
            assert f >= 0.0f && f <= 1.0f;
            m_scoreboard.setAnimation(f);
            m_scoreboard.repaint();
        }

        /** Display the right answer screen. */
        private void displayRightAnswer()
        {
            //getGlassPane().setVisible(false);

            lockButtons(true);
            QuizButton btn = m_frame.getSelectedButton();
            m_btnJokerAudi.setLocked(true);
            m_btnJokerFifty.setLocked(true);

            repaint();
        }

        /** Display the wrong answer screen. */
        private void displayWrongAnswer()
        {
            //getGlassPane().setVisible(false);

            QuizButton btn = m_frame.getSelectedButton();
            lockButtons(true);
            m_btnJokerAudi.setLocked(true);
            m_btnJokerFifty.setLocked(true);

            repaint();
        }

        /** Display the game over screen.
         * The last question is required to hightlight the
         * actual correct answer with a green glow.
         * @param q Last question object.
         * @param level Fallback level. */
        private void displayGameOver(QuizQuestion q, int level)
        {
            getGlassPane().setVisible(false);

            lockButtons(true);
            m_btnJokerAudi.setLocked(true);
            m_btnJokerFifty.setLocked(true);
            setScoreboardAnimation(0.0f);

            m_background.setBackground(m_imgBackgroundLost);
            m_scoreboard.setLevel(level);

            // highlight the correct answer,
            // so the user might learn something :)
            int correctanswer = q.getCorrectAnswer();
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
            {
                if(correctanswer == i) // highlight the true answer with the green glow.
                {
                    getButton(i).setRight(true);
                    continue;
                }
                // Remove the text of the other two buttons.
                if(getButton(i).getWrong() == false && correctanswer != i)
                    getButton(i).setText("");
            }

            QuizLanguage language = m_framework.getLanguage();
            String gameOverText = language.getString(QuizLanguage.StringID.LOST);
            setQuestionText(gameOverText);

            repaint();
        }

        /** Display the intro screen at the beginning of a game. */
        private void displayIntro()
        {
            getGlassPane().setVisible(false);

            // display the intro screen
            clearButtonEffects();
            m_scoreboard.setLevel(0);
            m_southPanel.setVisible(false);
            m_jokerPanel.setVisible(false);
            m_scoreboard.setVisible(false);
            m_background.setBackground(m_imgIntro);

            // reset joker buttons
            m_btnJokerAudi.setSelected(false);
            m_btnJokerFifty.setSelected(false);
            m_btnJokerAudi.setHover(false);
            m_btnJokerFifty.setHover(false);
            m_btnJokerAudi.setLocked(false);
            m_btnJokerFifty.setLocked(false);

            // set an empty string to the answer buttons
            lockButtons(true);
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
                getButton(i).setText("");

            QuizLanguage language = m_framework.getLanguage();
            if(language != null)
            {
                String introText = language.getString(QuizLanguage.StringID.INTRO);
                setQuestionText(introText);
            }

            repaint();
        }

        /** Display the winning screen.
         * @param q The winning question.
         * @param level The current level. */
        private void displayGameWon(QuizQuestion q, int level)
        {
            m_background.setBackground(Color.WHITE); // winning screen is white
            QuizGlassPane glasspane = (QuizGlassPane)getGlassPane();
            glasspane.setVisible(true); // display the winning effect
            glasspane.spawnParticles(200); // create particles
            glasspane.update(0.0f); // initialize particles

            lockButtons(true);
            m_btnJokerAudi.setLocked(true);
            m_btnJokerFifty.setLocked(true);

            setScoreboardAnimation(0.0f);
            m_background.setBackground(m_imgBackgroundWon);
            m_scoreboard.setLevel(level);

            // display the winning text (e.g. "YOU HAVE WON!")
            QuizLanguage language = m_framework.getLanguage();
            String winText = language.getString(QuizLanguage.StringID.WON);
            setQuestionText(winText);

            // remove all answers except the winning answer.
            int correctanswer = q.getCorrectAnswer();
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
            {
                if(correctanswer != i) // exclude the correct answer
                {
                    getButton(i).setText("");
                }
            }

            repaint();
        }

        /** Freeze/unfreeze the answer buttons so that the user can't click on them.
         * @param locked If true, all buttons ignore user actions. */
        private void lockButtons(boolean locked)
        {
            // lock/unlock all buttons
            for(int i=0;i<BUTTON_ANSWER_COUNT;i++)
                getButton(i).setLocked(locked);
        }

        /** event handler for buttons in this frame */
        public void mouseEntered(MouseEvent e) {}
        /** event handler for buttons in this frame */
        public void mouseExited(MouseEvent e) {}
        /** event handler for buttons in this frame */
        public void mousePressed(MouseEvent e) {}
        /** event handler for buttons in this frame */
        public void mouseReleased(MouseEvent e) {}
        /** event handler for buttons in this frame */
        public void mouseClicked(MouseEvent e)
        {
            Object obj = e.getSource();
            if(obj instanceof QuizButton) // thank you RTTI
            {
                QuizButton btn = (QuizButton)obj;
                if(btn.getLocked() == false)
                    fireEventButtonSelect(btn.getID());
            }
        }
    }

    /** The Quiz Glass Pane for overlay effects. */
    class QuizGlassPane extends JComponent
    {
        /** Constructor needs the resource of a particle bitmap.
         * @param particle Loaded image to draw the particles. */
        QuizGlassPane(BufferedImage particle)
        {
            m_particle = particle;
            m_particles = null;
            m_lastStateTime = 0.0f;
            m_statetime = 0.0f;
        }

        /** Remember the last statetime to calculate the time difference (dt)
         * for the movement integration. */
        private float m_lastStateTime = 0.0f;
        /** Time animation. */
        private float m_statetime = 0.0f;
        /** Particle bitmap. */
        private BufferedImage m_particle;
        /** particle gravity */
        private final float GRAVITY = 4.0f;
        /** particle array */
        private GlassParticle[] m_particles;

        /** Animate the glass pane. Set statetime to 0 to reset the animation.
         * @param statetime Time [s] of animation. */
        public void update(float statetime)
        {
            if(m_particles == null)
                return;

            m_statetime = statetime;
            float dt = m_statetime - m_lastStateTime;
            float height = (float)getHeight();

            /* when the state changes, our m_lastStateTime is invalid */
            if(m_statetime == 0.0f || dt < 0.0f)
            {
                dt = 0.0f;
            }

            for(GlassParticle p : m_particles)
            {
                p.x += p.vx*dt + 0.5f*p.ax*p.ax*dt;
                p.y += p.vy*dt + 0.5f*p.ay*p.ay*dt;
                p.vx += p.ax*dt;
                p.vy += p.ay*dt;

                // reset to the top of the screen
                if(p.y > height) {
                    p.x = p.startX;
                    p.y = p.startY;
                    p.vx = 0.0f;
                    p.vy = 0.0f;
                }
                p.ax = (float)Math.sin(p.omega*dt);
            }

            m_lastStateTime = m_statetime;
        }

        /** Paint function to display the glass pane effect for the winning
         * screen. This is a function of time. Call update(dt) to animate the
         * effects.
         * @param g Graphics context. */
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            if(m_particles == null || m_particles.length <= 0)
                return;

            Graphics2D g2d = (Graphics2D) g;
            // bilinear filtering for the stretched bitmap
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            for(GlassParticle p : m_particles)
            {
                // this is a hack, m_particles should be protected by a
                // mutex object
                if(p == null)
                    continue;

                g2d.drawImage(m_particle, (int)p.x, (int)p.y, p.size, p.size, this);
            }
        }

        /** Spawn particles.
         * @param number How many particles should be on the screen. */
        public void spawnParticles(int number)
        {
            Quiz.Print("Spawn particles: " + number);

            if(number < 1)
            {
                assert number >= 0;
                m_particles = null;
                return;
            }

            Random random = new Random();
            int width = getWidth();
            int height = getHeight();
            m_particles = new GlassParticle[number];
            for(int i=0;i<m_particles.length;i++)
            {
                m_particles[i] = new GlassParticle();
                m_particles[i].startX = random.nextInt(width);
                m_particles[i].x = m_particles[i].startX;

                // max size: 90 pixels
                m_particles[i].size = Math.min(20 + Math.abs((int)random.nextGaussian()*55), 90);
                m_particles[i].startY = -(random.nextInt(height)+m_particles[i].size)/10;
                //m_particles[i].startY = random.nextInt(height);
                m_particles[i].y = m_particles[i].startY;
                // normal distributed gravity noise with boundaries: 0.5*G to 4*G
                m_particles[i].ay = Math.abs(GRAVITY + (float)random.nextGaussian()*GRAVITY*0.5f);
                m_particles[i].ay = Math.min(m_particles[i].ay, 4*GRAVITY);
                m_particles[i].ay = Math.max(m_particles[i].ay, (float)0.5*GRAVITY);

                // add a periodic force on the x-axis, depending on omega and time.
                // force_x = f(omega, t)
                float T = (float)(random.nextGaussian()*0.1f);
                if(Math.abs(T) < 0.01f)
                    T = 0.01f;
                // limit the omega range to -10*G to 10*G
                m_particles[i].omega = Math.min(2.0f*(float)Math.PI*(1.0f/T), 10*GRAVITY);
                m_particles[i].omega = Math.max(m_particles[i].omega, -10*GRAVITY);
            }
        }

        /** This is a private class of QuizGlassPane to display
         * animated particles on the winning screen. */
        class GlassParticle
        {
            GlassParticle() {
            }

            /** start position on X axis */
            public int startX = 0;
            /** start position on Y axis */
            public int startY = 0;
            /** current particle position X on the screen */
            public float x = 0;
            /** current particle position Y on the screen */
            public float y = 0;
            /** size in pixels */
            public int size = 10;
            /** velocity x pixel/s */
            public float vx = 0;
            /** velocity y pixel/s */
            public float vy = 0;
            /** acceleration x pixel/s */
            public float ax = 0;
            /** acceleration y pixel/s */
            public float ay = 0;
            /** wind acceleration */
            public float omega = 0;
        }
    }
}

