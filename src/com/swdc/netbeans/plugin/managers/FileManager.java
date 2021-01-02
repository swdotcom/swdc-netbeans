/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.swdc.netbeans.plugin.managers;

import com.swdc.netbeans.plugin.SoftwareUtil;
import com.swdc.snowplow.tracker.entities.UIElementEntity;
import com.swdc.snowplow.tracker.events.UIInteractionType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;

public class FileManager {

    public static final Logger log = Logger.getLogger("FileManager");

    public static void openReadmeFile(UIInteractionType interactionType) {
        SwingUtilities.invokeLater(() -> {
            Project p = SoftwareUtil.getOpenProject();
            if (p == null) {
                return;
            }

            UIElementEntity elementEntity = new UIElementEntity();
            elementEntity.element_name = interactionType == UIInteractionType.click ? "ct_learn_more_btn" : "ct_learn_more_cmd";
            elementEntity.element_location = interactionType == UIInteractionType.click ? "ct_menu_tree" : "ct_command_palette";
            elementEntity.color = interactionType == UIInteractionType.click ? "yellow" : null;
            elementEntity.cta_text = "Learn more";
            elementEntity.icon_name = interactionType == UIInteractionType.click ? "document" : null;
            EventTrackerManager.getInstance().trackUIInteraction(interactionType, elementEntity);

            String fileContent = getReadmeContent();

            String readmeFile = SoftwareUtil.getReadmeFile();
            File f = new File(readmeFile);
            if (!f.exists()) {
                Writer writer = null;
                // write the summary content
                try {
                    writer = new BufferedWriter(new OutputStreamWriter(
                            new FileOutputStream(new File(readmeFile)), StandardCharsets.UTF_8));
                    writer.write(fileContent);
                } catch (IOException ex) {
                    // Report
                } finally {
                    try {
                        if (writer != null) {
                            writer.close();
                        }
                    } catch (Exception ex) {/*ignore*/}
                }
            }

            SoftwareUtil.launchFile(f.getPath());
        });
    }

    private static String getReadmeContent() {
        return "CODE TIME\n" +
                "---------\n" +
                "\n" +
                "Code Time is an open source plugin for automatic programming metrics and time-tracking. \n" +
                "\n" +
                "\n" +
                "GETTING STARTED\n" +
                "---------------\n" +
                "\n" +
                "1. Create your web account\n" +
                "\n" +
                "    The Code Time web app has data visualizations and settings you can customize, such as your work hours and office type for advanced time tracking. You can also connect Google Calendar to visualize your Code Time vs. meetings in a single calendar.\n" +
                "\n" +
                "    You can connect multiple code editors on multiple devices using the same email account.\n" +
                "\n" +
                "2. Track your progress during the day\n" +
                "\n" +
                "    Your status bar shows you in real-time how many hours and minutes you code each day. A rocket will appear if your code time exceeds your daily average on this day of the week.\n" +
                "\n" +
                "3. Check out your coding activity\n" +
                "\n" +
                "    To see an overview of your coding activity and project metrics, open the Code Time panel by clicking on the Code Time icon in your side bar.\n" +
                "\n" +
                "    In your Activity Metrics, your _code time_ is the total time you have spent in your editor today. Your _active code_ time is the total time you have been typing in your editor today. Each metric shows how you compare today to your average and the global average. Each average is calculated by day of week over the last 90 days (e.g. a Friday average is an average of all previous Fridays). You can also see your top files today by KPM (keystrokes per minute), keystrokes, and code time.\n" +
                "\n" +
                "    If you have a Git repository open, Contributors provides a breakdown of contributors to the current open project and their latest commits.\n" +
                "\n" +
                "4. Generate your Code Time dashboard\n" +
                "\n" +
                "    At the end of your first day, open Code Time in your side bar and click _View summary_ to open your dashboard in a new editor tab. Your dashboard summarizes your coding data—such as your code time by project, lines of code, and keystrokes per minute—today, yesterday, last week, and over the last 90 days.\n" +
                "\n" +
                "\n" +
                "WEB APP DATA VISUALIZATIONS\n" +
                "---------------------------\n" +
                "\n" +
                "Click \"See advanced metrics\" in the Code Time side bar or visit app.software.com to see more data visualizations. Here are a few examples of what you will see in your dashboard after your first week.\n" +
                "\n" +
                "* Active code time\n" +
                "\n" +
                "    Visualize your daily active code time. See productivity trends compared to weekly and monthly averages. See how you stack up against the Software community of over 100,000 developers.\n" +
                "\n" +
                "* Top projects\n" +
                "\n" +
                "    See how much time you spend per project per week. Get a breakdown of your top projects right in your dashboard.\n" +
                "\n" +
                "* Work-life balance\n" +
                "\n" +
                "    Connect your Google Calendar to visualize meeting time versus code time. See how much coding happens during work hours versus nights and weekends so you can find ways to improve your work-life balance.\n" +
                "\n" +
                "SAFE, SECURE, AND FREE\n" +
                "----------------------\n" +
                "\n" +
                "We never access your code: We do not process, send, or store your proprietary code. We only provide metrics about programming, and we make it easy to see the data we collect.\n" +
                "\n" +
                "Your data is private: We will never share your individually identifiable data with your boss. In the future, we will roll up data into groups and teams but we will keep your data anonymized.\n" +
                "\n" +
                "Free for you, forever: We provide 90 days of data history for free, forever. In the future, we will provide premium plans for advanced features and historical data access.\n" +
                "\n" +
                "Code Time also collects basic usage metrics to help us make informed decisions about our roadmap.\n" +
                "\n" +
                "\n" +
                "GET IN TOUCH\n" +
                "------------\n" +
                "\n" +
                "Enjoying Code Time? Let us know how it’s going by tweeting or following us at @software_hq.\n" +
                "\n" +
                "We recently released a new beta plugin, Music Time for Visual Studio Code, which helps you find your most productive songs for coding. You can learn more at software.com.\n" +
                "\n" +
                "Have any questions? Please email us at support@software.com and we’ll get back to you as soon as we can.\n";
    }

}
