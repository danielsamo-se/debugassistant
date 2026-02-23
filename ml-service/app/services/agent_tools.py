"""
Tool definitions for the debug agent
"""

from typing import List, Dict, Any
from langchain_core.tools import tool

from app.services.embedding_service import get_embedding_service
from app.services.similarity_search import get_similarity_search


@tool
def search_similar_errors(query: str) -> List[Dict[str, Any]]:
    """
    Search for similar error patterns in the FAISS index using semantic search.
    Use this when you need to find similar stack traces or error patterns.
    Returns solutions and explanations for similar errors.
    """
    embedding_service = get_embedding_service()
    similarity_search = get_similarity_search()

    embedding = embedding_service.embed(query)
    results = similarity_search.search(embedding, k=5)

    return [
        {
            "exception": r["metadata"].get("exception"),
            "framework": r["metadata"].get("framework"),
            "solution": r["metadata"].get("solution"),
            "score": round(r["score"], 3)
        }
        for r in results
    ]


@tool
def analyze_stack_trace(stack_trace: str) -> Dict[str, Any]:
    """
    Extract structured information from a stack trace.
    Use this as a first step to understand what kind of error you're dealing with.
    Returns exception type, framework, and key terms.
    """
    lines = stack_trace.strip().split("\n")
    first_line = lines[0] if lines else ""

    exception_type = None
    if "Exception" in first_line or "Error" in first_line:
        parts = first_line.split(":")
        if parts:
            exception_part = parts[0].strip()
            exception_type = exception_part.split(".")[-1]

    framework = "unknown"
    if "springframework" in stack_trace.lower():
        framework = "spring"
    elif "hibernate" in stack_trace.lower():
        framework = "hibernate"
    elif "jackson" in stack_trace.lower():
        framework = "jackson"

    return {
        "exception_type": exception_type,
        "framework": framework,
        "first_line": first_line[:200],
        "line_count": len(lines)
    }


@tool
def get_framework_best_practices(framework: str) -> str:
    """
    Get common solutions and best practices for a specific framework.
    Use this when you've identified the framework but need general guidance.
    Supported frameworks: spring, hibernate, jackson.
    """
    practices = {
        "spring": """
Spring Best Practices for Common Errors:
- NoSuchBeanDefinitionException: Add @Component, @Service, or @Repository annotation. Check @ComponentScan path.
- BeanCreationException (circular): Use @Lazy on one dependency or refactor to break cycle.
- UnsatisfiedDependencyException: Ensure all required beans are defined and annotated.
- PortInUseException: Change server.port in application.properties or kill process on port.
""",
        "hibernate": """
Hibernate Best Practices for Common Errors:
- LazyInitializationException: Use @Transactional on service method or JOIN FETCH in query.
- TransientPropertyValueException: Save child entity first or use CascadeType.PERSIST.
- OptimisticLockException: Implement retry logic or use pessimistic locking for high contention.
- MappingException: Ensure @Entity annotation and entity scan configuration.
""",
        "jackson": """
Jackson Best Practices for Common Errors:
- InvalidFormatException: Validate input format matches expected type. Use @JsonFormat for dates.
- JsonMappingException: Add public getters or @JsonAutoDetect. Check for circular references.
- UnrecognizedPropertyException: Add @JsonIgnoreProperties(ignoreUnknown=true) or define missing fields.
"""
    }

    return practices.get(framework.lower(), f"No specific best practices found for {framework}. Analyze the error message directly.")


def get_all_tools():
    """Return all available tools for the agent"""
    return [search_similar_errors, analyze_stack_trace, get_framework_best_practices]