package uwr.onlinejudge.server.controllers;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uwr.onlinejudge.server.models.*;
import uwr.onlinejudge.server.models.form.*;
import uwr.onlinejudge.server.services.GroupService;
import uwr.onlinejudge.server.services.SolutionService;
import uwr.onlinejudge.server.services.TaskService;
import uwr.onlinejudge.server.services.UserService;
import uwr.onlinejudge.server.util.Languages;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Controller
@RequestMapping
public class TaskController {
    private TaskService taskService;
    private UserService userService;
    private GroupService groupService;
    private SolutionService solutionService;

    @Autowired
    public TaskController(TaskService taskService, UserService userService, GroupService groupService, SolutionService solutionService) {
        this.taskService = taskService;
        this.userService = userService;
        this.groupService = groupService;
        this.solutionService = solutionService;
    }

    @RequestMapping(value = "/zadanie/{id}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String showTask(@PathVariable("id") Long id, Model model, Principal principal) {
        Task task = taskService.getTask(id);

        if (task == null)
            return "error_page";


        User user = userService.findByEmail(principal.getName());
        Collection<Test> tests = taskService.getTests(task);
        Collection<Solution> solutions = taskService.getSolutions(user, task);
        Collection<Languages> languages = task.getLanguages();


        Map<Languages, Boolean> allPossibleLanguages = Arrays.asList(Languages.values()).
                stream().
                collect(Collectors.toMap(Function.identity(), l -> languages.stream().anyMatch(s -> s.getId() == l.getId())));


        SolutionForm solutionForm = new SolutionForm(task);
        TestForm testForm = new TestForm(task);
        TaskLanguagesForm taskLanguagesForm = new TaskLanguagesForm(allPossibleLanguages);

        solutions = solutions.stream().sorted(Comparator.comparing(Solution::getDateOfSending).reversed()).collect(Collectors.toList());
        Solution lastSolution = solutions.stream().findFirst().orElse(null);


        model.addAttribute("solutionForm", solutionForm);
        model.addAttribute("testForm", testForm);
        task.getTaskDescription().setContent(Jsoup.parse(task.getTaskDescription().getContent()).outerHtml());
        model.addAttribute("task", task);

        model.addAttribute("tests", tests);
        model.addAttribute("solutions", solutions);
        model.addAttribute("languages", languages);
        model.addAttribute("allPossibleLanguages", allPossibleLanguages);
        model.addAttribute("lastSolution", lastSolution);


        if (tests.isEmpty() || languages.isEmpty()) {
            model.addAttribute("alertMessage", "Nie zostały zdefiniowane żadne testy lub języki zadania nie zostały sprecyzowane.");
        }

        return "task";
    }

    @RequestMapping(value = "/dodaj_liste", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String saveList(@ModelAttribute("taskListForm") @Valid TaskListForm taskListForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "forms/add_list";
        }
        taskListForm.setUser(userService.findByEmail(principal.getName()));
        taskService.save(taskListForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Lista została utworzona");
        return "redirect:/grupa/" + taskListForm.getGroup().getId();
    }

    @RequestMapping(value = "/dodaj_opis_zadania/{taskListId}", method = RequestMethod.POST)
    @PreAuthorize("isFullyAuthenticated()")
    public String addTaskDescription(@PathVariable("taskListId") Long taskListId, @ModelAttribute("taskDescription") @Valid TaskDescriptionForm taskDescriptionForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("taskListId", taskListId);
            return "forms/add_task_description";
        }

        taskDescriptionForm.setUser(userService.findByEmail(principal.getName()));
        taskService.save(taskDescriptionForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Opis zadania został zdefiniowany");
        return "redirect:/dodaj_zadanie_do_listy/" + taskListId;
    }

    @RequestMapping(value = "/dodaj_zadanie/{taskListId}/{taskDescriptionId}", method = RequestMethod.GET)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String addTask(@PathVariable("taskListId") Long taskListId, @PathVariable("taskDescriptionId") Long taskDescriptionId, Model model) {
        TaskDescription taskDescription = taskService.getTaskDescription(taskDescriptionId);
        TaskList taskList = taskService.getTaskList(taskListId);

        if (taskDescription == null || taskList == null)
            return "error_page";

        TaskForm taskForm = new TaskForm(taskDescription, taskList, taskDescription.getName());
        model.addAttribute("taskForm", taskForm);
        return "forms/add_task";
    }

    @RequestMapping(value = "/dodaj_zadanie", method = RequestMethod.POST)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public String saveTask(@ModelAttribute("taskForm") @Valid TaskForm taskForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "forms/add_task";
        }

        taskForm.setUser(userService.findByEmail(principal.getName()));
        taskService.save(taskForm);

        redirectAttributes.addFlashAttribute("alertMessage", "Zadanie zostało dodane");

        return "redirect:/grupa/" + taskForm.getTaskList().getGroup().getId();

    }

    @RequestMapping(value = "/dodaj_zadanie_do_listy/{taskListId}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String addTaskToTaskList(@PathVariable("taskListId") Long taskListId, Model model) {
        TaskList taskList = taskService.getTaskList(taskListId);

        if (taskList == null)
            return "error_page";

        Collection<TaskDescription> taskDescriptions = taskService.getTaskDescriptions();
        model.addAttribute("taskListId", taskListId);
        model.addAttribute("taskDescriptions", taskDescriptions);
        model.addAttribute("taskDescription", new TaskDescriptionForm());

        return "add_task_to_list";
    }

    @RequestMapping(value = "/wynik_testu/{id}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String showTestResult(@PathVariable("id") Long id, Model model) {
        Score score = taskService.getScore(id);
        if (score == null)
            return "error_page";

        model.addAttribute("log", score.getTestResult());
        model.addAttribute("title", "Wynik testu");

        return "log";
    }

    @RequestMapping(value = "/wyslij_zadanie", method = RequestMethod.POST)
    @PreAuthorize("isFullyAuthenticated()")
    public String saveSolution(@ModelAttribute("solutionForm") @Valid SolutionForm solutionForm, BindingResult bindingResult, Principal principal, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "forms/add_solution";
        }

        solutionForm.setUser(userService.findByEmail(principal.getName()));
        long solutionId = solutionService.save(solutionForm).getId();

        redirectAttributes.addFlashAttribute("alertMessage", "Zadanie zostało wysłane");
        redirectAttributes.addFlashAttribute("onlineJudge", true);
        return "redirect:/zadanie/" + solutionForm.getTask().getId() + "?s=" + solutionId;
    }

    @RequestMapping(value = "/wyslij_zadanie_ponownie/{id}", method = RequestMethod.GET)
    @PreAuthorize("isFullyAuthenticated()")
    public String saveSolutionAgain(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        Solution solution = solutionService.getSolution(id);

        if (solution == null)
            return "error_page";

        redirectAttributes.addFlashAttribute("alertMessage", "Zadanie zostało wysłane");
        redirectAttributes.addFlashAttribute("onlineJudge", true);
        return "redirect:/zadanie/" + solution.getTask().getId() + "?s=" + id;
    }


}