from dataclasses import dataclass
from typing import List, Dict, Any, Optional
from enum import Enum


@dataclass
class FileInfo:
    path: str
    size: int
    file_type: str


@dataclass
class ProjectInfo:
    project_type: str
    root_path: str
    files: List[FileInfo]


@dataclass
class HTTPRequest:
    method: str
    path: str
    query_params: Dict[str, str]
    body: Dict[str, Any]

@dataclass
class APIInfo:
    controller_name: str 
    method_name: str 
    code_pos: str # /path/to/file.java:L1-L3
    req: HTTPRequest
    references: List[str]



@dataclass
class SymbolInfo:
    name: str
    symbol_type: str  # class, method, field, variable
    file_path: str
    line_number: int
    definition: str
    scope: str 


class AnalysisStatus(Enum):
    SUCCESS = "success"
    FAIL = "fail"
    INCOMPLETE = "incomplete"

@dataclass
class AnalysisResult:
    api_info: APIInfo
    analysis: str   
    status: AnalysisStatus


@dataclass
class SummaryResult:
    total_apis: int
    api_summary: str
    recommendations: List[str]
    analysis_results: List[AnalysisResult]
