package br.com.naheroback.modules.practiceExams.services;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class ExamDomains {

    public record Domain(String name, Double minWeight, Double maxWeight) {
        static Domain at(String name, double weight) {
            return new Domain(name, weight, weight);
        }

        static Domain between(String name, double minWeight, double maxWeight) {
            return new Domain(name, minWeight, maxWeight);
        }
    }

    private static final Map<String, List<Domain>> BY_PRACTICE_EXAM_SLUG = Map.ofEntries(
            Map.entry("aws-cloud-practitioner-clf-02", List.of(
                    Domain.at("Cloud Concepts", 24),
                    Domain.at("Security and Compliance", 30),
                    Domain.at("Cloud Technology and Services", 34),
                    Domain.at("Billing, Pricing, and Support", 12))),

            Map.entry("aws-solutions-architect-associate-saa-c03", List.of(
                    Domain.at("Design Secure Architectures", 30),
                    Domain.at("Design Resilient Architectures", 26),
                    Domain.at("Design High-Performing Architectures", 24),
                    Domain.at("Design Cost-Optimized Architectures", 20))),

            Map.entry("aws-developer-associate-dva-c02", List.of(
                    Domain.at("Development with AWS Services", 32),
                    Domain.at("Security", 26),
                    Domain.at("Deployment", 24),
                    Domain.at("Troubleshooting and Optimization", 18))),

            Map.entry("aws-sysops-administrator-associate-soa-c02", List.of(
                    Domain.at("Monitoring, Logging, Analysis, Remediation, and Performance Optimization", 22),
                    Domain.at("Reliability and Business Continuity", 22),
                    Domain.at("Deployment, Provisioning, and Automation", 22),
                    Domain.at("Security and Compliance", 16),
                    Domain.at("Networking and Content Delivery", 18))),

            Map.entry("aws-solutions-architect-professional-sap-c02", List.of(
                    Domain.at("Design Solutions for Organizational Complexity", 26),
                    Domain.at("Design for New Solutions", 29),
                    Domain.at("Continuous Improvement for Existing Solutions", 25),
                    Domain.at("Accelerate Workload Migration and Modernization", 20))),

            Map.entry("aws-devops-engineer-professional-dop-c02", List.of(
                    Domain.at("SDLC Automation", 22),
                    Domain.at("Configuration Management and IaC", 17),
                    Domain.at("Resilient Cloud Solutions", 15),
                    Domain.at("Monitoring and Logging", 15),
                    Domain.at("Incident and Event Response", 14),
                    Domain.at("Security and Compliance", 17))),

            Map.entry("aws-security-specialty-scs-c02", List.of(
                    Domain.at("Detection", 16),
                    Domain.at("Incident Response", 14),
                    Domain.at("Infrastructure Security", 18),
                    Domain.at("Identity and Access Management", 20),
                    Domain.at("Data Protection", 18),
                    Domain.at("Security Foundations and Governance", 14))),

            Map.entry("aws-advanced-networking-specialty-ans-c01", List.of(
                    Domain.at("Network Design", 30),
                    Domain.at("Network Implementation", 26),
                    Domain.at("Network Management and Operation", 20),
                    Domain.at("Network Security, Compliance, and Governance", 24))),

            Map.entry("aws-machine-learning-specialty-mls-c01", List.of(
                    Domain.at("Data Engineering", 20),
                    Domain.at("Exploratory Data Analysis", 24),
                    Domain.at("Modeling", 36),
                    Domain.at("Machine Learning Implementation and Operations", 20))),

            Map.entry("microsoft-azure-fundamentals-az-900", List.of(
                    Domain.between("Describe cloud concepts", 25, 30),
                    Domain.between("Describe Azure architecture and services", 35, 40),
                    Domain.between("Describe Azure management and governance", 30, 35))),

            Map.entry("microsoft-azure-administrator-az-104", List.of(
                    Domain.between("Manage Azure identities and governance", 20, 25),
                    Domain.between("Implement and manage storage", 15, 20),
                    Domain.between("Deploy and manage Azure compute resources", 20, 25),
                    Domain.between("Implement and manage virtual networking", 15, 20),
                    Domain.between("Monitor and maintain Azure resources", 10, 15))),

            Map.entry("microsoft-azure-developer-az-204", List.of(
                    Domain.between("Develop Azure compute solutions", 25, 30),
                    Domain.between("Develop for Azure storage", 15, 20),
                    Domain.between("Implement Azure security", 15, 20),
                    Domain.between("Monitor, troubleshoot, and optimize Azure solutions", 5, 10),
                    Domain.between("Connect to and consume Azure services and third-party services", 20, 25))),

            Map.entry("microsoft-azure-solutions-architect-az-305", List.of(
                    Domain.between("Design identity, governance, and monitoring solutions", 25, 30),
                    Domain.between("Design data storage solutions", 20, 25),
                    Domain.between("Design business continuity solutions", 15, 20),
                    Domain.between("Design infrastructure solutions", 30, 35))),

            Map.entry("microsoft-azure-devops-engineer-az-400", List.of(
                    Domain.between("Design and implement processes and communications", 10, 15),
                    Domain.between("Design and implement a source control strategy", 10, 15),
                    Domain.between("Design and implement build and release pipelines", 50, 55),
                    Domain.between("Develop a security and compliance plan", 10, 15),
                    Domain.between("Implement an instrumentation strategy", 5, 10))),

            Map.entry("microsoft-azure-security-engineer-az-500", List.of(
                    Domain.between("Secure identity and access", 15, 20),
                    Domain.between("Secure networking", 20, 25),
                    Domain.between("Secure compute, storage, and databases", 20, 25),
                    Domain.between("Secure Azure using Microsoft Defender for Cloud and Microsoft Sentinel", 30, 35))),

            Map.entry("microsoft-azure-ai-engineer-ai-102", List.of(
                    Domain.between("Plan and manage an Azure AI solution", 20, 25),
                    Domain.between("Implement generative AI solutions", 15, 20),
                    Domain.between("Implement an agentic solution", 5, 10),
                    Domain.between("Implement computer vision solutions", 10, 15),
                    Domain.between("Implement natural language processing solutions", 15, 20),
                    Domain.between("Implement knowledge mining and information extraction solutions", 15, 20))),

            Map.entry("microsoft-azure-data-scientist-dp-100", List.of(
                    Domain.between("Design and prepare a machine learning solution", 20, 25),
                    Domain.between("Explore data, and run experiments", 20, 25),
                    Domain.between("Train and deploy models", 25, 30),
                    Domain.between("Optimize language models for AI applications", 25, 30))),

            Map.entry("microsoft-azure-database-administrator-dp-300", List.of(
                    Domain.between("Plan and implement data platform resources", 15, 20),
                    Domain.between("Implement a secure environment", 20, 25),
                    Domain.between("Monitor, configure, and optimize database resources", 20, 25),
                    Domain.between("Configure and manage automation of tasks", 15, 20),
                    Domain.between("Plan and configure a high availability and disaster recovery (HA/DR) environment", 20, 25))),

            Map.entry("google-cloud-digital-leader", List.of(
                    Domain.at("Digital Transformation with Google Cloud", 17),
                    Domain.at("Exploring Data Transformation with Google Cloud", 16),
                    Domain.at("Innovating with Google Cloud Artificial Intelligence", 16),
                    Domain.at("Modernize Infrastructure and Applications with Google Cloud", 17),
                    Domain.at("Trust and Security with Google Cloud", 17),
                    Domain.at("Scaling with Google Cloud Operations", 17))),

            Map.entry("google-cloud-engineer-associate", List.of(
                    Domain.at("Setting up a cloud solution environment", 20),
                    Domain.at("Planning and configuring a cloud solution", 17.5),
                    Domain.at("Deploying and implementing a cloud solution", 25),
                    Domain.at("Ensuring successful operation of a cloud solution", 20),
                    Domain.at("Configuring access and security", 17.5))),

            Map.entry("google-cloud-architect-professional", List.of(
                    Domain.at("Designing and planning a cloud solution architecture", 25),
                    Domain.at("Managing and provisioning a cloud solution infrastructure", 17.5),
                    Domain.at("Designing for security and compliance", 17.5),
                    Domain.at("Analyzing and optimizing technical and business processes", 15),
                    Domain.at("Managing implementation", 12.5),
                    Domain.at("Ensuring solution and operations excellence", 12.5))),

            Map.entry("google-cloud-data-engineer-professional", List.of(
                    Domain.at("Designing data processing systems", 22),
                    Domain.at("Ingesting and processing the data", 25),
                    Domain.at("Storing the data", 20),
                    Domain.at("Preparing and using data for analysis", 15),
                    Domain.at("Maintaining and automating data workloads", 18))),

            Map.entry("google-cloud-database-engineer-professional", List.of(
                    Domain.at("Design innovative, scalable, and highly available cloud database solutions", 32),
                    Domain.at("Manage a solution that can span multiple database technologies", 25),
                    Domain.at("Migrate data solutions", 23),
                    Domain.at("Deploy scalable and highly available databases in Google Cloud", 20))),

            Map.entry("google-cloud-developer-professional", List.of(
                    Domain.at("Designing highly scalable, secure, and reliable cloud-native applications", 32),
                    Domain.at("Building and testing applications", 23),
                    Domain.at("Configuring cloud-native applications for deployment", 24),
                    Domain.at("Integrating applications with Google Cloud services", 21))),

            Map.entry("google-cloud-devops-engineer-professional", List.of(
                    Domain.at("Bootstrapping and maintaining a Google Cloud organization", 20),
                    Domain.at("Building and implementing CI/CD pipelines, including continuous testing, for application, infrastructure, and machine learning workloads", 25),
                    Domain.at("Applying site reliability engineering practices", 18),
                    Domain.at("Implementing observability practices and troubleshooting issues", 25),
                    Domain.at("Optimizing performance and cost", 12))),

            Map.entry("google-cloud-machine-learning-engineer-professional", List.of(
                    Domain.at("Architecting low-code AI solutions", 13),
                    Domain.at("Collaborating within and across teams to manage data and models", 16),
                    Domain.at("Scaling prototypes into ML models", 21),
                    Domain.at("Serving and scaling models", 20),
                    Domain.at("Automating and orchestrating ML pipelines", 18),
                    Domain.at("Monitoring AI solutions", 13))),

            Map.entry("google-cloud-network-engineer-professional", List.of(
                    Domain.at("Designing and planning a Google Cloud VPC network", 21),
                    Domain.at("Implementing a VPC network", 20),
                    Domain.at("Configuring managed network services", 16),
                    Domain.at("Configuring and implementing hybrid and multicloud network interconnectivity", 16),
                    Domain.at("Managing, monitoring, and troubleshooting network operations", 14),
                    Domain.at("Configuring, implementing and managing a cloud network security solution", 13))),

            Map.entry("google-cloud-security-engineer-professional", List.of(
                    Domain.at("Configuring access", 25),
                    Domain.at("Securing communications and establishing boundary protection", 22),
                    Domain.at("Ensuring data protection", 23),
                    Domain.at("Managing operations", 19),
                    Domain.at("Supporting compliance requirements", 11))),

            Map.entry("google-workspace-administrator", List.of(
                    Domain.at("Managing objects", 20),
                    Domain.at("Configuring services", 18),
                    Domain.at("Troubleshooting", 24),
                    Domain.at("Data access and authentication", 24),
                    Domain.at("Supporting business initiatives", 14)))
    );

    public boolean hasPracticeExam(String practiceExamSlug) {
        return practiceExamSlug != null && BY_PRACTICE_EXAM_SLUG.containsKey(practiceExamSlug);
    }

    public List<Domain> forPracticeExam(String practiceExamSlug) {
        return BY_PRACTICE_EXAM_SLUG.getOrDefault(practiceExamSlug, List.of());
    }

    public Set<String> mappedPracticeExamSlugs() {
        return BY_PRACTICE_EXAM_SLUG.keySet();
    }

    public Optional<String> canonicalName(String practiceExamSlug, String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        String normalised = candidate.trim();
        return forPracticeExam(practiceExamSlug).stream()
                .map(Domain::name)
                .filter(name -> name.equalsIgnoreCase(normalised))
                .findFirst();
    }
}
