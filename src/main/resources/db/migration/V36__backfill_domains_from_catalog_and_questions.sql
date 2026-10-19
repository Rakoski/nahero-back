CREATE TEMPORARY TABLE domain_catalog
(
    slug VARCHAR(255) NOT NULL,
    name VARCHAR(160) NOT NULL,
    min_weight NUMERIC(5, 2) NOT NULL,
    max_weight NUMERIC(5, 2) NOT NULL
) ON COMMIT DROP;

INSERT INTO domain_catalog (slug, name, min_weight, max_weight) VALUES
    ('aws-cloud-practitioner-clf-02', 'Cloud Concepts', 24, 24),
    ('aws-cloud-practitioner-clf-02', 'Security and Compliance', 30, 30),
    ('aws-cloud-practitioner-clf-02', 'Cloud Technology and Services', 34, 34),
    ('aws-cloud-practitioner-clf-02', 'Billing, Pricing, and Support', 12, 12),
    ('aws-solutions-architect-associate-saa-c03', 'Design Secure Architectures', 30, 30),
    ('aws-solutions-architect-associate-saa-c03', 'Design Resilient Architectures', 26, 26),
    ('aws-solutions-architect-associate-saa-c03', 'Design High-Performing Architectures', 24, 24),
    ('aws-solutions-architect-associate-saa-c03', 'Design Cost-Optimized Architectures', 20, 20),
    ('aws-developer-associate-dva-c02', 'Development with AWS Services', 32, 32),
    ('aws-developer-associate-dva-c02', 'Security', 26, 26),
    ('aws-developer-associate-dva-c02', 'Deployment', 24, 24),
    ('aws-developer-associate-dva-c02', 'Troubleshooting and Optimization', 18, 18),
    ('aws-sysops-administrator-associate-soa-c02', 'Monitoring, Logging, Analysis, Remediation, and Performance Optimization', 22, 22),
    ('aws-sysops-administrator-associate-soa-c02', 'Reliability and Business Continuity', 22, 22),
    ('aws-sysops-administrator-associate-soa-c02', 'Deployment, Provisioning, and Automation', 22, 22),
    ('aws-sysops-administrator-associate-soa-c02', 'Security and Compliance', 16, 16),
    ('aws-sysops-administrator-associate-soa-c02', 'Networking and Content Delivery', 18, 18),
    ('aws-solutions-architect-professional-sap-c02', 'Design Solutions for Organizational Complexity', 26, 26),
    ('aws-solutions-architect-professional-sap-c02', 'Design for New Solutions', 29, 29),
    ('aws-solutions-architect-professional-sap-c02', 'Continuous Improvement for Existing Solutions', 25, 25),
    ('aws-solutions-architect-professional-sap-c02', 'Accelerate Workload Migration and Modernization', 20, 20),
    ('aws-devops-engineer-professional-dop-c02', 'SDLC Automation', 22, 22),
    ('aws-devops-engineer-professional-dop-c02', 'Configuration Management and IaC', 17, 17),
    ('aws-devops-engineer-professional-dop-c02', 'Resilient Cloud Solutions', 15, 15),
    ('aws-devops-engineer-professional-dop-c02', 'Monitoring and Logging', 15, 15),
    ('aws-devops-engineer-professional-dop-c02', 'Incident and Event Response', 14, 14),
    ('aws-devops-engineer-professional-dop-c02', 'Security and Compliance', 17, 17),
    ('aws-security-specialty-scs-c02', 'Detection', 16, 16),
    ('aws-security-specialty-scs-c02', 'Incident Response', 14, 14),
    ('aws-security-specialty-scs-c02', 'Infrastructure Security', 18, 18),
    ('aws-security-specialty-scs-c02', 'Identity and Access Management', 20, 20),
    ('aws-security-specialty-scs-c02', 'Data Protection', 18, 18),
    ('aws-security-specialty-scs-c02', 'Security Foundations and Governance', 14, 14),
    ('aws-advanced-networking-specialty-ans-c01', 'Network Design', 30, 30),
    ('aws-advanced-networking-specialty-ans-c01', 'Network Implementation', 26, 26),
    ('aws-advanced-networking-specialty-ans-c01', 'Network Management and Operation', 20, 20),
    ('aws-advanced-networking-specialty-ans-c01', 'Network Security, Compliance, and Governance', 24, 24),
    ('aws-machine-learning-specialty-mls-c01', 'Data Engineering', 20, 20),
    ('aws-machine-learning-specialty-mls-c01', 'Exploratory Data Analysis', 24, 24),
    ('aws-machine-learning-specialty-mls-c01', 'Modeling', 36, 36),
    ('aws-machine-learning-specialty-mls-c01', 'Machine Learning Implementation and Operations', 20, 20),
    ('microsoft-azure-fundamentals-az-900', 'Describe cloud concepts', 25, 30),
    ('microsoft-azure-fundamentals-az-900', 'Describe Azure architecture and services', 35, 40),
    ('microsoft-azure-fundamentals-az-900', 'Describe Azure management and governance', 30, 35),
    ('microsoft-azure-administrator-az-104', 'Manage Azure identities and governance', 20, 25),
    ('microsoft-azure-administrator-az-104', 'Implement and manage storage', 15, 20),
    ('microsoft-azure-administrator-az-104', 'Deploy and manage Azure compute resources', 20, 25),
    ('microsoft-azure-administrator-az-104', 'Implement and manage virtual networking', 15, 20),
    ('microsoft-azure-administrator-az-104', 'Monitor and maintain Azure resources', 10, 15),
    ('microsoft-azure-developer-az-204', 'Develop Azure compute solutions', 25, 30),
    ('microsoft-azure-developer-az-204', 'Develop for Azure storage', 15, 20),
    ('microsoft-azure-developer-az-204', 'Implement Azure security', 15, 20),
    ('microsoft-azure-developer-az-204', 'Monitor, troubleshoot, and optimize Azure solutions', 5, 10),
    ('microsoft-azure-developer-az-204', 'Connect to and consume Azure services and third-party services', 20, 25),
    ('microsoft-azure-solutions-architect-az-305', 'Design identity, governance, and monitoring solutions', 25, 30),
    ('microsoft-azure-solutions-architect-az-305', 'Design data storage solutions', 20, 25),
    ('microsoft-azure-solutions-architect-az-305', 'Design business continuity solutions', 15, 20),
    ('microsoft-azure-solutions-architect-az-305', 'Design infrastructure solutions', 30, 35),
    ('microsoft-azure-devops-engineer-az-400', 'Design and implement processes and communications', 10, 15),
    ('microsoft-azure-devops-engineer-az-400', 'Design and implement a source control strategy', 10, 15),
    ('microsoft-azure-devops-engineer-az-400', 'Design and implement build and release pipelines', 50, 55),
    ('microsoft-azure-devops-engineer-az-400', 'Develop a security and compliance plan', 10, 15),
    ('microsoft-azure-devops-engineer-az-400', 'Implement an instrumentation strategy', 5, 10),
    ('microsoft-azure-security-engineer-az-500', 'Secure identity and access', 15, 20),
    ('microsoft-azure-security-engineer-az-500', 'Secure networking', 20, 25),
    ('microsoft-azure-security-engineer-az-500', 'Secure compute, storage, and databases', 20, 25),
    ('microsoft-azure-security-engineer-az-500', 'Secure Azure using Microsoft Defender for Cloud and Microsoft Sentinel', 30, 35),
    ('microsoft-azure-ai-engineer-ai-102', 'Plan and manage an Azure AI solution', 20, 25),
    ('microsoft-azure-ai-engineer-ai-102', 'Implement generative AI solutions', 15, 20),
    ('microsoft-azure-ai-engineer-ai-102', 'Implement an agentic solution', 5, 10),
    ('microsoft-azure-ai-engineer-ai-102', 'Implement computer vision solutions', 10, 15),
    ('microsoft-azure-ai-engineer-ai-102', 'Implement natural language processing solutions', 15, 20),
    ('microsoft-azure-ai-engineer-ai-102', 'Implement knowledge mining and information extraction solutions', 15, 20),
    ('microsoft-azure-data-scientist-dp-100', 'Design and prepare a machine learning solution', 20, 25),
    ('microsoft-azure-data-scientist-dp-100', 'Explore data, and run experiments', 20, 25),
    ('microsoft-azure-data-scientist-dp-100', 'Train and deploy models', 25, 30),
    ('microsoft-azure-data-scientist-dp-100', 'Optimize language models for AI applications', 25, 30),
    ('microsoft-azure-database-administrator-dp-300', 'Plan and implement data platform resources', 15, 20),
    ('microsoft-azure-database-administrator-dp-300', 'Implement a secure environment', 20, 25),
    ('microsoft-azure-database-administrator-dp-300', 'Monitor, configure, and optimize database resources', 20, 25),
    ('microsoft-azure-database-administrator-dp-300', 'Configure and manage automation of tasks', 15, 20),
    ('microsoft-azure-database-administrator-dp-300', 'Plan and configure a high availability and disaster recovery (HA/DR) environment', 20, 25),
    ('google-cloud-digital-leader', 'Digital Transformation with Google Cloud', 17, 17),
    ('google-cloud-digital-leader', 'Exploring Data Transformation with Google Cloud', 16, 16),
    ('google-cloud-digital-leader', 'Innovating with Google Cloud Artificial Intelligence', 16, 16),
    ('google-cloud-digital-leader', 'Modernize Infrastructure and Applications with Google Cloud', 17, 17),
    ('google-cloud-digital-leader', 'Trust and Security with Google Cloud', 17, 17),
    ('google-cloud-digital-leader', 'Scaling with Google Cloud Operations', 17, 17),
    ('google-cloud-engineer-associate', 'Setting up a cloud solution environment', 20, 20),
    ('google-cloud-engineer-associate', 'Planning and configuring a cloud solution', 17.5, 17.5),
    ('google-cloud-engineer-associate', 'Deploying and implementing a cloud solution', 25, 25),
    ('google-cloud-engineer-associate', 'Ensuring successful operation of a cloud solution', 20, 20),
    ('google-cloud-engineer-associate', 'Configuring access and security', 17.5, 17.5),
    ('google-cloud-architect-professional', 'Designing and planning a cloud solution architecture', 25, 25),
    ('google-cloud-architect-professional', 'Managing and provisioning a cloud solution infrastructure', 17.5, 17.5),
    ('google-cloud-architect-professional', 'Designing for security and compliance', 17.5, 17.5),
    ('google-cloud-architect-professional', 'Analyzing and optimizing technical and business processes', 15, 15),
    ('google-cloud-architect-professional', 'Managing implementation', 12.5, 12.5),
    ('google-cloud-architect-professional', 'Ensuring solution and operations excellence', 12.5, 12.5),
    ('google-cloud-data-engineer-professional', 'Designing data processing systems', 22, 22),
    ('google-cloud-data-engineer-professional', 'Ingesting and processing the data', 25, 25),
    ('google-cloud-data-engineer-professional', 'Storing the data', 20, 20),
    ('google-cloud-data-engineer-professional', 'Preparing and using data for analysis', 15, 15),
    ('google-cloud-data-engineer-professional', 'Maintaining and automating data workloads', 18, 18),
    ('google-cloud-database-engineer-professional', 'Design innovative, scalable, and highly available cloud database solutions', 32, 32),
    ('google-cloud-database-engineer-professional', 'Manage a solution that can span multiple database technologies', 25, 25),
    ('google-cloud-database-engineer-professional', 'Migrate data solutions', 23, 23),
    ('google-cloud-database-engineer-professional', 'Deploy scalable and highly available databases in Google Cloud', 20, 20),
    ('google-cloud-developer-professional', 'Designing highly scalable, secure, and reliable cloud-native applications', 32, 32),
    ('google-cloud-developer-professional', 'Building and testing applications', 23, 23),
    ('google-cloud-developer-professional', 'Configuring cloud-native applications for deployment', 24, 24),
    ('google-cloud-developer-professional', 'Integrating applications with Google Cloud services', 21, 21),
    ('google-cloud-devops-engineer-professional', 'Bootstrapping and maintaining a Google Cloud organization', 20, 20),
    ('google-cloud-devops-engineer-professional', 'Building and implementing CI/CD pipelines, including continuous testing, for application, infrastructure, and machine learning workloads', 25, 25),
    ('google-cloud-devops-engineer-professional', 'Applying site reliability engineering practices', 18, 18),
    ('google-cloud-devops-engineer-professional', 'Implementing observability practices and troubleshooting issues', 25, 25),
    ('google-cloud-devops-engineer-professional', 'Optimizing performance and cost', 12, 12),
    ('google-cloud-machine-learning-engineer-professional', 'Architecting low-code AI solutions', 13, 13),
    ('google-cloud-machine-learning-engineer-professional', 'Collaborating within and across teams to manage data and models', 16, 16),
    ('google-cloud-machine-learning-engineer-professional', 'Scaling prototypes into ML models', 21, 21),
    ('google-cloud-machine-learning-engineer-professional', 'Serving and scaling models', 20, 20),
    ('google-cloud-machine-learning-engineer-professional', 'Automating and orchestrating ML pipelines', 18, 18),
    ('google-cloud-machine-learning-engineer-professional', 'Monitoring AI solutions', 13, 13),
    ('google-cloud-network-engineer-professional', 'Designing and planning a Google Cloud VPC network', 21, 21),
    ('google-cloud-network-engineer-professional', 'Implementing a VPC network', 20, 20),
    ('google-cloud-network-engineer-professional', 'Configuring managed network services', 16, 16),
    ('google-cloud-network-engineer-professional', 'Configuring and implementing hybrid and multicloud network interconnectivity', 16, 16),
    ('google-cloud-network-engineer-professional', 'Managing, monitoring, and troubleshooting network operations', 14, 14),
    ('google-cloud-network-engineer-professional', 'Configuring, implementing and managing a cloud network security solution', 13, 13),
    ('google-cloud-security-engineer-professional', 'Configuring access', 25, 25),
    ('google-cloud-security-engineer-professional', 'Securing communications and establishing boundary protection', 22, 22),
    ('google-cloud-security-engineer-professional', 'Ensuring data protection', 23, 23),
    ('google-cloud-security-engineer-professional', 'Managing operations', 19, 19),
    ('google-cloud-security-engineer-professional', 'Supporting compliance requirements', 11, 11),
    ('google-workspace-administrator', 'Managing objects', 20, 20),
    ('google-workspace-administrator', 'Configuring services', 18, 18),
    ('google-workspace-administrator', 'Troubleshooting', 24, 24),
    ('google-workspace-administrator', 'Data access and authentication', 24, 24),
    ('google-workspace-administrator', 'Supporting business initiatives', 14, 14);

INSERT INTO domains (name)
SELECT DISTINCT ON (LOWER(c.name)) c.name
FROM domain_catalog c
WHERE NOT EXISTS (SELECT 1 FROM domains d WHERE LOWER(d.name) = LOWER(c.name))
ORDER BY LOWER(c.name), c.name;

INSERT INTO domains (name)
SELECT DISTINCT ON (LOWER(TRIM(q.domain))) TRIM(q.domain)
FROM questions q
WHERE q.domain IS NOT NULL
  AND TRIM(q.domain) <> ''
  AND NOT EXISTS (SELECT 1 FROM domains d WHERE LOWER(d.name) = LOWER(TRIM(q.domain)))
ORDER BY LOWER(TRIM(q.domain)), TRIM(q.domain);

INSERT INTO practice_exam_domains (practice_exam_id, domain_id, min_weight, max_weight)
SELECT pe.id, d.id, c.min_weight, c.max_weight
FROM domain_catalog c
JOIN practice_exams pe ON pe.slug = c.slug
JOIN domains d ON LOWER(d.name) = LOWER(c.name)
ON CONFLICT (practice_exam_id, domain_id) DO NOTHING;

INSERT INTO practice_exam_domains (practice_exam_id, domain_id)
SELECT DISTINCT q.practice_exam_id, d.id
FROM questions q
JOIN domains d ON LOWER(d.name) = LOWER(TRIM(q.domain))
WHERE q.practice_exam_id IS NOT NULL
ON CONFLICT (practice_exam_id, domain_id) DO NOTHING;

UPDATE questions q
SET domain_id = d.id
FROM domains d
WHERE q.domain_id IS NULL
  AND q.domain IS NOT NULL
  AND LOWER(d.name) = LOWER(TRIM(q.domain));
